#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PJ_DIR="$ROOT_DIR/.build/pjproject"
PJPROJECT_REPO_DEFAULT="https://github.com/pjsip/pjproject.git"
PJPROJECT_REPO="${PJPROJECT_REPO:-$PJPROJECT_REPO_DEFAULT}"
PJPROJECT_REF_DEFAULT="master"
PJPROJECT_REF="${PJPROJECT_REF:-$PJPROJECT_REF_DEFAULT}"
NDK_VERSION_DEFAULT="28.2.13676358"
ANDROID_NDK_ROOT_DEFAULT="$HOME/Library/Android/sdk/ndk/$NDK_VERSION_DEFAULT"
ANDROID_NDK_ROOT="${ANDROID_NDK_ROOT:-$ANDROID_NDK_ROOT_DEFAULT}"
JAVA_HOME_DEFAULT="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
ABIS_CSV_DEFAULT="arm64-v8a,armeabi-v7a,x86,x86_64"
ABIS_CSV="${ABIS:-$ABIS_CSV_DEFAULT}"
APP_PLATFORM="${APP_PLATFORM:-21}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  --pj-ref <ref>          pjproject git ref (tag/branch/commit), default: ${PJPROJECT_REF_DEFAULT}
  --pj-repo <url>         pjproject repo URL, default: ${PJPROJECT_REPO_DEFAULT}
  --abis <csv>            Comma-separated ABIs, default: ${ABIS_CSV_DEFAULT}
  --app-platform <api>    Android APP_PLATFORM, default: 21
  --help                  Show this help message

Environment overrides:
  PJPROJECT_REF, PJPROJECT_REPO, ABIS, APP_PLATFORM, ANDROID_NDK_ROOT, JAVA_HOME

Example:
  ./scripts/rebuild_pjsua2_android_16kb.sh --pj-ref 2.15.1 --abis arm64-v8a,x86_64
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --pj-ref)
      PJPROJECT_REF="$2"
      shift 2
      ;;
    --pj-repo)
      PJPROJECT_REPO="$2"
      shift 2
      ;;
    --abis)
      ABIS_CSV="$2"
      shift 2
      ;;
    --app-platform)
      APP_PLATFORM="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

IFS=',' read -r -a ABIS <<< "$ABIS_CSV"

if [[ ${#ABIS[@]} -eq 0 ]]; then
  echo "No ABIs selected."
  exit 1
fi

cpu_count() {
  if command -v nproc >/dev/null 2>&1; then
    nproc
  elif command -v sysctl >/dev/null 2>&1; then
    sysctl -n hw.ncpu 2>/dev/null || echo 4
  else
    echo 4
  fi
}

validate_alignment() {
  local lib_file="$1"
  local abi="$2"

  if [[ "$abi" != "arm64-v8a" && "$abi" != "x86_64" ]]; then
    return 0
  fi

  if ! command -v objdump >/dev/null 2>&1; then
    echo "objdump not found; skipping alignment validation for $lib_file"
    return 0
  fi

  local align_values
  align_values="$(objdump -p "$lib_file" | awk '/LOAD/ {print $NF}')"
  if [[ -z "$align_values" ]]; then
    echo "Failed to parse ELF LOAD alignment for $lib_file"
    return 1
  fi

  while IFS= read -r align; do
    if [[ "$align" =~ 2\*\*([0-9]+) ]]; then
      if (( ${BASH_REMATCH[1]} < 14 )); then
        echo "Invalid alignment for $lib_file: $align (needs >= 2**14 for 64-bit ABIs)"
        return 1
      fi
    fi
  done <<< "$align_values"

  return 0
}

if [[ ! -d "$ANDROID_NDK_ROOT" ]]; then
  echo "ANDROID_NDK_ROOT not found: $ANDROID_NDK_ROOT"
  echo "Set ANDROID_NDK_ROOT before running this script."
  exit 1
fi

if ! command -v swig >/dev/null 2>&1; then
  echo "swig is required. Install with: brew install swig"
  exit 1
fi

mkdir -p "$ROOT_DIR/.build"
if [[ ! -d "$PJ_DIR/.git" ]]; then
  git clone "$PJPROJECT_REPO" "$PJ_DIR"
fi

pushd "$PJ_DIR" >/dev/null

git fetch --tags origin
git checkout "$PJPROJECT_REF"

echo "Using pjproject ref: $(git rev-parse --short HEAD)"

export ANDROID_NDK_ROOT
export JAVA_HOME

write_config_site() {
  cat > pjlib/include/pj/config_site.h <<'EOF'
#define PJ_CONFIG_ANDROID 1
#include <pj/config_site_sample.h>
#define PJMEDIA_HAS_VIDEO 1
EOF
}

for abi in "${ABIS[@]}"; do
  echo "=== Building $abi ==="
  make distclean >/dev/null 2>&1 || true
  write_config_site

  APP_PLATFORM="$APP_PLATFORM" TARGET_ABI="$abi" ./configure-android --use-ndk-cflags
  make dep
  make clean >/dev/null 2>&1 || true
  make -j"$(cpu_count)"

  make -C pjsip-apps/src/swig/java clean >/dev/null 2>&1 || true
  make -C pjsip-apps/src/swig/java -j1

  src_lib="pjsip-apps/src/swig/java/android/pjsua2/src/main/jniLibs/$abi/libpjsua2.so"
  if [[ -f "$src_lib" ]]; then
    mkdir -p "$ROOT_DIR/android/src/main/jniLibs/$abi"
    cp -f "$src_lib" "$ROOT_DIR/android/src/main/jniLibs/$abi/libpjsua2.so"
    validate_alignment "$ROOT_DIR/android/src/main/jniLibs/$abi/libpjsua2.so" "$abi"
    echo "Copied: $src_lib -> android/src/main/jniLibs/$abi/libpjsua2.so"
  else
    echo "Missing build output for $abi: $src_lib"
    exit 1
  fi

done

popd >/dev/null

echo "Done. Rebuilt and copied libpjsua2.so for: ${ABIS[*]}"
