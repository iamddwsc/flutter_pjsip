import 'package:flutter/material.dart';
import 'package:flutter_pjsip/flutter_pjsip.dart';

/// Demo script để test gọi trực tiếp đến SIP account local
///
/// Hướng dẫn sử dụng:
/// 1. Đảm bảo máy Mac đã chạy: pjsua --id "sip:ddwsc@192.168.1.138" --no-udp --local-port 5080
/// 2. Chạy app Flutter này từ điện thoại/simulator trên cùng network
/// 3. Nhấn "Test Local SIP Call" để gọi trực tiếp đến account ddwsc

class LocalSipTestPage extends StatefulWidget {
  @override
  _LocalSipTestPageState createState() => _LocalSipTestPageState();
}

class _LocalSipTestPageState extends State<LocalSipTestPage> {
  late FlutterPjsip _pjsip;
  String _statusText = 'Ready to test';
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _initializePjsip();
  }

  void _initializePjsip() {
    _pjsip = FlutterPjsip.instance;
    _pjsip.onSipStateChanged.listen((map) {
      final state = map['call_state'];
      setState(() {
        _statusText = 'Call State: $state';
      });
      print('SIP State: $state');
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Local SIP Test'),
        backgroundColor: Colors.teal,
      ),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Local SIP UA Info:',
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      SizedBox(height: 8),
                      Text('IP: 192.168.1.138'),
                      Text('Port: 5080 (TCP/UDP)'),
                      Text('Target User: ddwsc'),
                      Text(
                          'SIP URI: sip:ddwsc@192.168.1.138:5080 (trying UDP first)'),
                      SizedBox(height: 8),
                      Text(
                        'Mode: Direct P2P calling (no registration server)',
                        style: TextStyle(
                            color: Colors.blue, fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 20),
              ElevatedButton(
                onPressed: !_isInitialized ? _initializeSip : null,
                child: Text('1. Initialize PJSIP'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue,
                  padding: EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              SizedBox(height: 10),
              ElevatedButton(
                onPressed: _isInitialized ? _checkPjsipState : null,
                child: Text('Check PJSIP State'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.purple,
                  padding: EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              SizedBox(height: 10),
              ElevatedButton(
                onPressed: _isInitialized ? _callLocalSipUserDirect : null,
                child: Text('2. Call Local SIP User (Direct)'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  padding: EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              SizedBox(height: 10),
              Text(
                'Note: Calling directly without registration (Local SIP UA)',
                style: TextStyle(
                    color: Colors.orange,
                    fontStyle: FontStyle.italic,
                    fontSize: 12),
              ),
              SizedBox(height: 10),
              ElevatedButton(
                onPressed: _isInitialized ? _hangupCall : null,
                child: Text('Hangup Call'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  padding: EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              SizedBox(height: 20),
              Card(
                color: Colors.grey[100],
                child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Status:',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 8),
                      Text(_statusText),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 20),
              Text(
                'Instructions:\n'
                '1. Make sure pjsua is running on Mac with UDP support:\n'
                '   pjsua --id "sip:ddwsc@192.168.1.138" --local-port 5080\n'
                '   (Removed --no-udp to enable UDP transport)\n'
                '2. Connect your device to the same network (192.168.1.x)\n'
                '3. Initialize PJSIP\n'
                '4. Call directly (will try UDP first, then TCP fallback)\n'
                '5. Answer the call on Mac terminal by typing "a" + Enter\n\n'
                'Note: This is peer-to-peer SIP calling, not server-based registration.',
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _initializeSip() async {
    try {
      setState(() => _statusText = 'Initializing PJSIP...');

      bool success = await _pjsip.pjsipInit();

      setState(() {
        _isInitialized = success;
        _statusText = success
            ? 'PJSIP initialized successfully'
            : 'Failed to initialize PJSIP';
      });
    } catch (e) {
      setState(() => _statusText = 'Error initializing: $e');
    }
  }

  Future<void> _checkPjsipState() async {
    try {
      setState(() => _statusText = 'Checking PJSIP state...');

      Map<dynamic, dynamic> stateInfo = await _pjsip.pjsipCheckState();
      print('PJSIP State: $stateInfo');

      String stateDetails = 'PJSIP State: ${stateInfo['state_name']}\n'
          'Running: ${stateInfo['is_running'] == 1 ? 'YES' : 'NO'}\n'
          'Accounts: ${stateInfo['account_count']}\n'
          'Ready for direct calling: ${stateInfo['is_running'] == 1 ? 'YES' : 'NO'}';

      setState(() => _statusText = stateDetails);
    } catch (e) {
      setState(() => _statusText = 'Error checking state: $e');
    }
  }

  Future<void> _callLocalSipUserDirect() async {
    try {
      setState(() => _statusText = 'Checking PJSIP state...');

      // Check PJSIP state first
      Map<dynamic, dynamic> stateInfo = await _pjsip.pjsipCheckState();
      print('PJSIP State: $stateInfo');

      // Fix: Check for 1 (number) instead of true (boolean)
      if (stateInfo['is_running'] != 1) {
        setState(() => _statusText =
            'Error: PJSIP not running (state: ${stateInfo['is_running']}). Please initialize first.');
        return;
      }

      String stateDetails =
          'PJSIP State: ${stateInfo['state_name']}, Accounts: ${stateInfo['account_count']}';
      setState(() => _statusText =
          '$stateDetails\nCalling ddwsc@192.168.1.138 directly...');

      // Call directly without registration - try UDP first, then TCP fallback
      bool success =
          await _pjsip.pjsipCallDirectUri('sip:ddwsc@192.168.1.138:5080');

      // If UDP fails, try TCP as fallback
      if (!success) {
        setState(
            () => _statusText = '$stateDetails\nUDP failed, trying TCP...');
        success = await _pjsip
            .pjsipCallDirectUri('sip:ddwsc@192.168.1.138:5080;transport=TCP');
      }

      setState(() {
        _statusText = success
            ? '$stateDetails\nDirect call initiated to ddwsc\n\nNext: Answer call on Mac terminal by typing "a" + Enter'
            : '$stateDetails\nFailed to initiate direct call';
      });
    } catch (e) {
      setState(() => _statusText = 'Error calling: $e');
    }
  }

  Future<void> _hangupCall() async {
    try {
      setState(() => _statusText = 'Hanging up...');

      bool success = await _pjsip.pjsipRefuse();

      setState(() {
        _statusText = success ? 'Call ended' : 'Failed to hangup';
      });
    } catch (e) {
      setState(() => _statusText = 'Error hanging up: $e');
    }
  }

  @override
  void dispose() {
    _pjsip.dispose();
    super.dispose();
  }
}

// Helper widget để chạy demo
class LocalSipTestApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Local SIP Test',
      theme: ThemeData(
        primarySwatch: Colors.teal,
      ),
      home: LocalSipTestPage(),
    );
  }
}

// Main function để chạy riêng app test này
// void main() => runApp(LocalSipTestApp());
