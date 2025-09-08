import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_pjsip/flutter_pjsip.dart';
import 'package:flutter_pjsip_example/local_sip_test.dart';
import 'package:fluttertoast/fluttertoast.dart';

void main() => runApp(LocalSipTestApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _calltateText = '';
  late FlutterPjsip _pjsip;

  @override
  void initState() {
    super.initState();
    initSipPlugin();
  }

  void initSipPlugin() {
    _pjsip = FlutterPjsip.instance;
    _pjsip.onSipStateChanged.listen((map) {
      final state = map['call_state'];
      // final remoteUri = map['remote_uri'];
      print('收到状态: $state');
      switch (state) {
        case "CALLING":
          break;

        case "INCOMING":
          break;

        case "EARLY":
          break;

        case "CONNECTING":
          break;

        case "CONFIRMED":
          break;

        case "DISCONNECTED":
          break;

        default:
          break;
      }

      setState(() {
        this._calltateText = state;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            ElevatedButton(
              child: Text('Sip初始化'),
              onPressed: () => _sipInit(),
            ),
            ElevatedButton(
              child: Text('Sip登录'),
              onPressed: () => _sipLogin(),
            ),
            ElevatedButton(
              child: Text('Sip打电话'),
              onPressed: () => _sipCall(),
            ),
            ElevatedButton(
              child: Text('Sip登出'),
              onPressed: () => _sipLogout(),
            ),
            ElevatedButton(
              child: Text('Sip销毁'),
              onPressed: () => _sipDeinit(),
            ),
            ElevatedButton(
              child: Text('Sip接听'),
              onPressed: () => _sipReceive(),
            ),
            ElevatedButton(
              child: Text('Sip拒接/挂断'),
              onPressed: () => _sipRefuse(),
            ),
            ElevatedButton(
              child: Text('Sip免提'),
              onPressed: () => _sipHandsFree(),
            ),
            ElevatedButton(
              child: Text('Sip静音'),
              onPressed: () => _sipMute(),
            ),
            ElevatedButton(
              child: Text('Sip通道销毁'),
              onPressed: () => _sipDispose(),
            ),
            SizedBox(height: 20),
            Text('=== 本地SIP测试 ===',
                style: TextStyle(fontWeight: FontWeight.bold)),
            ElevatedButton(
              child: Text('登录本地SIP (192.168.1.138)'),
              onPressed: () => _sipLoginLocal(),
            ),
            ElevatedButton(
              child: Text('呼叫本地用户 ddwsc'),
              onPressed: () => _sipCallLocalUser(),
            ),
            Text('电话状态监听：$_calltateText'),
          ],
        ),
      ),
    );
  }

  Future<void> _sipInit() async {
    bool initSuccess = await _pjsip.pjsipInit();
    showToast('初始化', initSuccess);
  }

  Future<void> _sipLogin() async {
    bool loginSuccess = await _pjsip.pjsipLogin(
        username: '1012',
        password: '123@jvtd',
        ip: '117.78.34.48',
        port: '6050');
    showToast('登录', loginSuccess);
  }

  Future<void> _sipCall() async {
    bool callSuccess = await _pjsip.pjsipCall(
        username: '1010', ip: '117.78.34.48', port: '6050');
    showToast('打电话', callSuccess);
  }

  Future<void> _sipLogout() async {
    bool logoutSuccess = await _pjsip.pjsipLogout();
    showToast('登出', logoutSuccess);
  }

  Future<void> _sipDeinit() async {
    bool initSuccess = await _pjsip.pjsipDeinit();
    showToast('销毁', initSuccess);
  }

  Future<void> _sipReceive() async {
    bool receiveSuccess = await _pjsip.pjsipReceive();
    showToast('接听', receiveSuccess);
  }

  Future<void> _sipRefuse() async {
    bool refuseSuccess = await _pjsip.pjsipRefuse();
    showToast('拒接/挂断', refuseSuccess);
  }

  Future<void> _sipHandsFree() async {
    bool handsFreeSuccess = await _pjsip.pjsipHandsFree(true);
    showToast('免提状态更改', handsFreeSuccess);
  }

  Future<void> _sipMute() async {
    bool muteSuccess = await _pjsip.pjsipMute();
    showToast('静音状态更改', muteSuccess);
  }

  Future<void> _sipDispose() async {
    await _pjsip.dispose();
    showToast('通道销毁', true);
  }

  // 本地SIP测试方法
  Future<void> _sipLoginLocal() async {
    // 登录到本地SIP服务器，作为另一个用户（比如 testuser）
    bool loginSuccess = await _pjsip.pjsipLogin(
        username: 'testuser',
        password: '', // 本地测试通常不需要密码
        ip: '192.168.1.138',
        port: '5080');
    showToast('本地登录', loginSuccess);
  }

  Future<void> _sipCallLocalUser() async {
    // 直接呼叫ddwsc用户
    bool callSuccess = await _pjsip
        .pjsipCallDirectUri('sip:ddwsc@192.168.1.138:5080;transport=TCP');
    showToast('呼叫本地用户', callSuccess);
  }

  void showToast(String method, bool success) {
    String successText = success ? '成功' : '失败';
    Fluttertoast.showToast(msg: '$method $successText');
  }
}
