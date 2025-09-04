# Audio Session Fixes for PJSIP Flutter Plugin

## Vấn đề được giải quyết

Lỗi `AVAudioSessionClient_Common.mm:600 Failed to set properties, error: '!pri'` thường xảy ra do:

1. **Thiếu xử lý lỗi đúng cách** - Các API audio session có thể fail và cần được xử lý
2. **Sử dụng API deprecated** - `AudioSessionSetProperty` đã bị deprecated từ iOS 7
3. **Conflict giữa các cấu hình audio session** - Không deactivate session trước khi thay đổi
4. **Buffer duration không phù hợp** - 5ms quá nhỏ cho VoIP calls

## Những thay đổi đã thực hiện

### 1. Cập nhật `configAudioSession:`
- Thêm proper error handling
- Deactivate session trước khi cấu hình
- Sử dụng các options phù hợp cho VoIP:
  - `AVAudioSessionCategoryOptionAllowBluetooth`
  - `AVAudioSessionCategoryOptionAllowBluetoothA2DP` 
  - `AVAudioSessionCategoryOptionDefaultToSpeaker`
- Tăng buffer duration lên 20ms (phù hợp cho VoIP)
- Activate session sau khi cấu hình xong

### 2. Thay thế `resetAudioSession`
- Loại bỏ deprecated `AudioSessionSetProperty`
- Sử dụng modern `AVAudioSession` APIs
- Thêm proper error handling
- Reset về default category và mode

### 3. Cải thiện `setAudioSession`
- Thêm error handling
- Sử dụng proper options cho từng trường hợp
- Activate session sau mỗi thay đổi

### 4. Cải thiện `enableSpeakerForCall:`
- Loại bỏ `AVAudioSessionCategoryOptionMixWithOthers` (không phù hợp cho VoIP)
- Set mode thành `AVAudioSessionModeVoiceChat`
- Better error handling

### 5. Thêm `setupAudioSessionForCall`
- Method chuyên dụng để setup audio session cho VoIP calls
- Được gọi trước khi make call và receive call
- Cấu hình tối ưu cho voice chat:
  - Sample rate: 16kHz
  - Buffer duration: 20ms
  - Mode: VoiceChat
  - Proper category options

## Cách sử dụng

Các method đã được tự động integrate vào flow hiện tại:

```objc
// Khi gọi ra
- (void)dailWithPhonenumber:(NSString *)phonenumber {
    // Tự động setup audio session
    [self setupAudioSessionForCall];
    // ... rest of the code
}

// Khi có cuộc gọi đến
- (void)handleIncommingCall:(NSNotification *)notification {
    // Tự động setup audio session
    [self setupAudioSessionForCall];
    // ... rest of the code
}
```

## Best Practices được áp dụng

1. **Always handle errors** - Tất cả audio session APIs đều có error handling
2. **Deactivate before major changes** - Tránh conflicts
3. **Use appropriate options** - Options phù hợp với VoIP calls
4. **Proper buffer duration** - 20ms thay vì 5ms
5. **Activate after configuration** - Đảm bảo changes được apply

## Lưu ý

- Nếu vẫn gặp lỗi, kiểm tra `Info.plist` xem đã có permissions chưa:
  ```xml
  <key>NSMicrophoneUsageDescription</key>
  <string>This app needs microphone access for voice calls</string>
  ```

- Đảm bảo app được test trên device thật, không phải simulator

- Monitor console logs để xem có error messages nào từ audio session không
