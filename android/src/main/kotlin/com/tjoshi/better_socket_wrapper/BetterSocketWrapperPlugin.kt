package com.tjoshi.better_socket_wrapper

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.security.*
import java.security.cert.*
import javax.net.ssl.*

/** BetterSocketWrapperPlugin */
class BetterSocketWrapperPlugin: FlutterPlugin, MethodCallHandler {

  lateinit var channel : MethodChannel
  var betterWebSocketClient: BetterWebSocketClient? = null
  val queuingEventSink: QueuingEventSink = QueuingEventSink()
  lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "better_socket_wrapper")
    channel.setMethodCallHandler(this)
    //val plugin = BetterSocketExtentionPlugin();
    context=flutterPluginBinding.applicationContext
    EventChannel(flutterPluginBinding.binaryMessenger,"better_socket_wrapper/event").setStreamHandler(object :EventChannel.StreamHandler{
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        queuingEventSink.setDelegate(events)
      }

      override fun onCancel(arguments: Any?) {
        queuingEventSink.setDelegate(null)
      }
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
      "connentSocket" -> {
        val path = call.argument<String>("path")
        val httpHeaders = call.argument<Map<String, String>>("httpHeaders")
        val keyStorePath = call.argument<String>("keyStorePath")
        val keyPassword = call.argument<String>("keyPassword")
        val storePassword = call.argument<String>("storePassword")
        val keyStoreType = call.argument<String>("keyStoreType")
        val trustAllHost = call.argument<Boolean>("trustAllHost")?: false
        val webSocketUri = URI.create(path)
        close()
        betterWebSocketClient = BetterWebSocketClient(webSocketUri, queuingEventSink, httpHeaders = httpHeaders)
        if (keyStorePath?.isNotEmpty()==true&&keyPassword?.isNotEmpty()==true&&storePassword?.isNotEmpty()==true&&keyStoreType?.isNotEmpty()==true){
          val sslFactory = getSSLContextFromAndroidKeystore(context,storePassword,keyPassword,keyStorePath,keyStoreType).socketFactory
          betterWebSocketClient?.setSocketFactory(sslFactory)
        }
        if (trustAllHost){
          betterWebSocketClient?.setSocketFactory(getSSLContext().socketFactory)
        }
        betterWebSocketClient?.connect()
        result.success(null)
      }
      "sendMsg" -> {
        val msg = call.argument<String>("msg")
        if (betterWebSocketClient?.isOpen == true)
          betterWebSocketClient?.send(msg)
        result.success(null)
      }
      "sendByteMsg" -> {
        val msg = call.argument<ByteArray>("msg")
        if (betterWebSocketClient?.isOpen == true)
          betterWebSocketClient?.send(msg)
        result.success(null)
      }
      "close" -> {
        close()
        result.success(null)
      }
      else -> result.notImplemented()
    }
  }

  private fun close() {
    if (betterWebSocketClient?.isOpen == true) {
      betterWebSocketClient?.close()
    }
    betterWebSocketClient = null
  }

  private fun getSSLContext(): SSLContext {
    val x509TrustManager = object : X509TrustManager {
      override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
      }

      override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
      }

      override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
      }
    }

    val sslContext: SSLContext
    try {
      sslContext = SSLContext.getInstance("TLS")
      sslContext.init(null, arrayOf<TrustManager>(x509TrustManager), null)
    } catch (e: KeyStoreException) {
      throw IllegalArgumentException()
    } catch (e: IOException) {
      throw IllegalArgumentException()
    } catch (e: CertificateException) {
      throw IllegalArgumentException()
    } catch (e: NoSuchAlgorithmException) {
      throw IllegalArgumentException()
    } catch (e: KeyManagementException) {
      throw IllegalArgumentException()
    } catch (e: UnrecoverableKeyException) {
      throw IllegalArgumentException()
    }
    return sslContext
  }

  private fun getSSLContextFromAndroidKeystore(context: Context, storePassword:String, keyPassword:String, keyStorePath:String, keyStoreType:String = "BKS"): SSLContext {
    // load up the key store
    val sslContext: SSLContext
    try {
      val keystore: KeyStore = KeyStore.getInstance(keyStoreType)
      val inputStream: InputStream = context.assets.open(keyStorePath)
      inputStream.use { _inputStream ->
        keystore.load(_inputStream, storePassword.toCharArray())
      }
      val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("X509")
      keyManagerFactory.init(keystore, keyPassword.toCharArray())
      val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("X509")
      tmf.init(keystore)
      sslContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, null)
    } catch (e: KeyStoreException) {
      throw IllegalArgumentException()
    } catch (e: IOException) {
      throw IllegalArgumentException()
    } catch (e: CertificateException) {
      throw IllegalArgumentException()
    } catch (e: NoSuchAlgorithmException) {
      throw IllegalArgumentException()
    } catch (e: KeyManagementException) {
      throw IllegalArgumentException()
    } catch (e: UnrecoverableKeyException) {
      throw IllegalArgumentException()
    }
    return sslContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
