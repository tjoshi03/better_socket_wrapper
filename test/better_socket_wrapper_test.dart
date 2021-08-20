import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:better_socket_wrapper/better_socket_wrapper.dart';

void main() {
  const MethodChannel channel = MethodChannel('better_socket_wrapper');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await BetterSocketWrapper.platformVersion, '42');
  });
}
