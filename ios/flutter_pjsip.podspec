#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_pjsip'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin.'
  s.description      = <<-DESC
A new Flutter plugin.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*.h', 'Classes/**/*.m'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.resources = ['Classes/PJSIPClass/Assets/*.png','Classes/PJSIPClass/Assets/*.wav', 'Classes/PJSIPClass/Assets/*.mp3']
  # s.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }
  s.dependency 'pjsip'
  s.dependency 'Masonry','~> 0.6.3'
  s.ios.deployment_target = '8.0'
end

