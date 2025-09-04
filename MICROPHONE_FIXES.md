# Sửa lỗi "Bên kia không nghe được tiếng mình nói" trên iOS

## Vấn đề

Khách hàng phản ánh khi gọi bằng iOS thì bên đầu dây bên kia không nghe thấy tiếng mình nói.

## Nguyên nhân chính

1. **Echo Cancellation bị tắt**: `media_cfg.ec_tail_len = 0` làm tắt echo cancellation
2. **Thiếu microphone permission check**: Không kiểm tra quyền microphone
3. **Audio conference connection không đầy đủ**: Chỉ kết nối khi `PJSUA_CALL_MEDIA_ACTIVE`
4. **Thiếu audio routing sau khi call established**: Không đảm bảo audio routing đúng sau khi cuộc gọi kết nối

## Các thay đổi đã thực hiện

### 1. Cải thiện Media Configuration
```objc
// Enable echo cancellation (quan trọng cho chất lượng microphone)
media_cfg.ec_tail_len = PJSUA_DEFAULT_EC_TAIL_LEN;
media_cfg.ec_options = 0;

// Audio settings for better quality
media_cfg.quality = 4;
media_cfg.ptime = 20;
media_cfg.no_vad = PJ_FALSE; // Enable Voice Activity Detection
```

### 2. Cải thiện Audio Conference Connection
```objc
static void on_call_media_state(pjsua_call_id call_id) {
    // Connect cả hai chiều: speakers/earpiece ← call → microphone
    pj_status_t status1 = pjsua_conf_connect(ci.conf_slot, 0); // Call to speaker
    pj_status_t status2 = pjsua_conf_connect(0, ci.conf_slot); // Microphone to call
    
    // Thêm logging để debug
    // Ensure audio routing sau khi connect
}
```

### 3. Thêm Microphone Permission Check
```objc
- (BOOL)setupAudioSessionForCall {
    // Check microphone permission trước khi setup
    if ([session recordPermission] != AVAudioSessionRecordPermissionGranted) {
        [session requestRecordPermission:^(BOOL granted) {
            if (!granted) {
                NSLog(@"Microphone permission denied!");
            }
        }];
    }
}
```

### 4. Thêm Audio Routing Enforcement
```objc
- (void)ensureProperAudioRouting {
    // Đảm bảo category và mode đúng cho VoIP
    // Check microphone permission
    // Activate audio session
    // Log trạng thái audio session
}
```

### 5. Thêm Call State Handling
```objc
- (void)handleCllStatusChanged:(NSNotification *)notification {
    if (state == PJSIP_INV_STATE_CONFIRMED) {
        // Call active - ensure audio routing
        [PJSipManager.shared ensureProperAudioRouting];
        // Test microphone sau 1 giây
        [PJSipManager.shared testMicrophone];
    }
}
```

### 6. Thêm Microphone Testing
```objc
- (void)testMicrophone {
    // Check conference port connections
    // Check signal levels
    // Log audio routing information
}
```

### 7. Cải thiện Call Answer
```objc
-(void)incommingCallReceive{
    [self setupAudioSessionForCall]; // Setup trước khi answer
    pjsua_call_answer(_call_id, 200, NULL, NULL);
    // Ensure routing sau khi answer
}
```

## Các cải thiện bổ sung

### Audio Session Options
- Thêm `AVAudioSessionCategoryOptionDuckOthers`
- Set `AVAudioSessionModeVoiceChat` (quan trọng cho microphone processing)
- Set preferred input channels = 1 (mono)

### Logging
- Thêm extensive logging cho debug
- Log audio session properties
- Log conference port connections
- Log signal levels

### Error Handling
- Better error handling cho tất cả audio operations
- Continue processing ngay cả khi có warnings

## Cách test

1. **Kiểm tra permissions**: App sẽ log microphone permission status
2. **Kiểm tra audio routing**: Logs sẽ hiển thị audio session properties
3. **Kiểm tra conference connections**: Method `testMicrophone` sẽ log connection status
4. **Monitor signal levels**: Logs sẽ hiển thị TX/RX levels

## Lưu ý quan trọng

1. **Test trên device thật**: Simulator không có microphone thật
2. **Check Info.plist**: Đảm bảo có `NSMicrophoneUsageDescription`
3. **Monitor logs**: Quan sát console logs để debug issues
4. **Test với nhiều scenarios**: Gọi ra, nhận cuộc gọi, speaker/earpiece

## Expected Results

- Bên kia sẽ nghe được tiếng mình nói rõ ràng
- Echo cancellation hoạt động tốt
- Audio quality được cải thiện
- Microphone hoạt động ổn định trên tất cả devices
