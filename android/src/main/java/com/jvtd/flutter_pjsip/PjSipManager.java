package com.jvtd.flutter_pjsip;

import com.jvtd.flutter_pjsip.entity.MyAccount;
import com.jvtd.flutter_pjsip.entity.MyCall;
import com.jvtd.flutter_pjsip.interfaces.MyAppObserver;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.SipHeader;
import org.pjsip.pjsua2.SipHeaderVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Description: PjSip管理类
 * Author: Jack Zhang
 * create on: 2019-08-21 23:19
 */
public class PjSipManager
{
  private static volatile PjSipManager mInstance;
  private AccountConfig mAccountConfig;
  private MyAccount mAccount;

  static
  {
    try
    {
      System.loadLibrary("openh264");
      // Ticket #1937: libyuv is now included as static lib
      //System.loadLibrary("yuv");
    } catch (UnsatisfiedLinkError e)
    {
      System.out.println("UnsatisfiedLinkError: " + e.getMessage());
      System.out.println("This could be safely ignored if you don't need video.");
    }
    try
    {
      System.loadLibrary("pjsua2");
      System.out.println("Library loaded");
    } catch (Exception e)
    {
      e.printStackTrace();
    } catch (Error error)
    {
      error.printStackTrace();
    }
  }

  public static PjSipManager getInstance()
  {
    if (mInstance == null)
      synchronized (PjSipManager.class)
      {
        if (mInstance == null)
          mInstance = new PjSipManager();
      }
    return mInstance;
  }

  private PjSipManager()
  {

  }

  public static Endpoint mEndPoint;
  public static MyAppObserver observer;

  /**
   * 初始化方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 14:34
   */
  public void init(MyAppObserver obs)
  {
    init(obs, false);
  }

  /**
   * 初始化方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 14:34
   */
  public void init(MyAppObserver obs, boolean own_worker_thread)
  {
    observer = obs;

    /* Create endpoint */
    try
    {
      if (mEndPoint == null)
        mEndPoint = new Endpoint();
      mEndPoint.libCreate();
    } catch (Exception e)
    {
      return;
    }

    EpConfig epConfig = new EpConfig();

    // UAConfig，指定核心SIP用户代理设置
    UaConfig ua_cfg = epConfig.getUaConfig();
    ua_cfg.setUserAgent("Pjsua2 Android " + mEndPoint.libVersion().getFull());

    /* STUN server. */
    //StringVector stun_servers = new StringVector();
    //stun_servers.add("stun.pjsip.org");
    //ua_cfg.setStunServer(stun_servers);

    /* No worker thread */
    if (own_worker_thread)
    {
      ua_cfg.setThreadCnt(0);
      ua_cfg.setMainThreadOnly(true);
    }

    // 指定ep_cfg中设置的自定义
    try
    {
      mEndPoint.libInit(epConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
      return;
    }

    TransportConfig sipTpConfig = new TransportConfig();
    int SIP_PORT = 6050;

    /* Set SIP port back to default for JSON saved config */
    sipTpConfig.setPort(SIP_PORT);

    // 创建一个或多个传输
    try
    {
      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
      return;
    }
//    try
//    {
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//    }
//
//    try
//    {
//      sipTpConfig.setPort(SIP_PORT + 1);
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//    }

    /* Start. */
    try
    {
      mEndPoint.libStart();
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void handleNetworkChange()
  {
    try
    {
      System.out.println("Network change detected");
      IpChangeParam changeParam = new IpChangeParam();
      mEndPoint.handleIpChange(changeParam);
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void deinit()
  {
    /* Try force GC to avoid late destroy of PJ objects as they should be
     * deleted before lib is destroyed.
     */
    Runtime.getRuntime().gc();

    /* Shutdown pjsua. Note that Endpoint destructor will also invoke
     * libDestroy(), so this will be a test of double libDestroy().
     */
    try
    {
      mEndPoint.libDestroy();
    } catch (Exception e)
    {
      e.printStackTrace();
    }

    /* Force delete Endpoint here, to avoid deletion from a non-
     * registered thread (by GC?).
     */
    mEndPoint.delete();
    mEndPoint = null;
  }

  public void login(String username, String password, String ip, String port)
  {
    mAccountConfig = new AccountConfig();
    mAccountConfig.getNatConfig().setIceEnabled(true);
    // 未实现视频功能，先置位false
    mAccountConfig.getVideoConfig().setAutoTransmitOutgoing(false);// 自动向外传输视频流
    mAccountConfig.getVideoConfig().setAutoShowIncoming(false);// 自动接收并显示来的视频流
    mAccountConfig.setIdUri("sip:" + username + "@" + ip + ":" + port);
    mAccountConfig.getRegConfig().setRegistrarUri("sip:" + ip + ":" + port);
    AuthCredInfoVector creds = mAccountConfig.getSipConfig().getAuthCreds();
    if (creds != null)
    {
      creds.clear();
      if (username != null && username.length() != 0)
        creds.add(new AuthCredInfo("Digest", "*", username, 0, password));
    }

    mAccount = new MyAccount(mAccountConfig);
    try
    {
      mAccount.create(mAccountConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
      mAccount = null;
    }
  }

  public boolean loginWithInfo(Map<String, Object> info) {
    SipHeaderVector sipHeaders = new SipHeaderVector();
    SipHeader sipHeader = new SipHeader();
    sipHeader.setHName("Call-ID");
//        Random random = new Random();
    sipHeader.setHValue(UUID.randomUUID().toString());
    AccountConfig accCfg = new AccountConfig();


//        accCfg.setIdUri("sip:nhcla152@103.57.210.247:51000");
    accCfg.getNatConfig().setIceEnabled(false);

    accCfg.getVideoConfig().setAutoTransmitOutgoing(false);
    accCfg.getVideoConfig().setAutoShowIncoming(true);

    String sipURI = "sip:";
    String addTransport = "";
    String port = "";
    int serverPort = (int) info.get("serverPort");
    if ((int) info.get("serverPort") > 0)
      port = ":" + info.serverPort;
//        int switch_port = 51000;
    if (!TextUtils.isEmpty(info.serverType))
      addTransport = ";transport=" + info.serverType.toLowerCase();
    String sipid = sipURI + info.phoneLine + "@" + info.serverUrl /*+ ":" + switch_port*/;
    String registrarstr = sipURI + info.serverUrl + port + addTransport;


    accCfg.getNatConfig().setContactRewriteUse(0);
    accCfg.getNatConfig().setContactRewriteMethod(0);
    accCfg.getNatConfig().setContactUseSrcPort(0);
    accCfg.getNatConfig().setViaRewriteUse(0);
    accCfg.getNatConfig().setSipStunUse(PJSUA_STUN_USE_DISABLED);
    accCfg.getMediaConfig().setIpv6Use(PJSUA_IPV6_DISABLED);

    accCfg.setIdUri(sipid);

    sipHeaders.add(sipHeader);
    accCfg.getRegConfig().setHeaders(sipHeaders);
    accCfg.getRegConfig().setRegistrarUri(registrarstr);
    accCfg.getPresConfig().setHeaders(sipHeaders);
    AuthCredInfoVector creds = accCfg.getSipConfig().getAuthCreds();

    creds.clear();

    creds.add(new AuthCredInfo("Digest", "*", info.phoneLine, PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue(), info.phoneLinePassword));
    StringVector proxies = accCfg.getSipConfig().getProxies();
    proxies.clear();
    if (!TextUtils.isEmpty(info.outboundProxy)) {
      String proxystr = sipURI + info.outboundProxy /*+ ":" + port + addTransport*/;
      proxies.add(proxystr);
    }
    accCfg.getSipConfig().setProxies(proxies);
    /* Enable ICE */
//        accCfg.getNatConfig().setIceEnabled(false);
    if (account != null) {
      account.delete();
      account = null;
    }
    account = new MyAccount(accCfg, info.serverId, info.phoneLine);

    try {
      account.create(accCfg);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (account == null) {
      throw new RuntimeException("Lỗi login");
    }

    long startTime = System.currentTimeMillis();
    boolean isRegistered = false;
    do {
//                    AccountInfo accountInfo = null;
      Lifecycle.State state = getLifecycle().getCurrentState();
      if (state.equals(Lifecycle.State.DESTROYED)) {
        return false;
      }
      try {
//                        accountInfo = account.getInfo();
        pjsip_status_code code = account.last_status_code;//accountInfo.getRegStatus();
        if (code.swigValue() / 100 == 2) {
          //online
          return true;
        } else if (code == pjsip_status_code.PJSIP_SC_PROGRESS) {
          //connecting...
        } else {
          //offline
          return false;
        }
        Log.d(this, "RegisterCode: " + code);
//                        isRegistered = code == pjsip_status_code.PJSIP_SC_OK;//accountInfo != null && accountInfo.getRegStatus() == pjsip_status_code.PJSIP_SC_OK;
      } catch (Throwable ex) {
        ex.printStackTrace();
      }

      if (!isRegistered) SystemClock.sleep(500);
    } while (!isRegistered && System.currentTimeMillis() - startTime < 30000);
    return isRegistered;
  }
//    return false;
//  }

  public MyCall call(String username, String ip, String port)
  {
    MyCall call = new MyCall(mAccount, -1);
    CallOpParam prm = new CallOpParam(true);
//    prm.getOpt().setAudioCount(1);
//    prm.getOpt().setVideoCount(1);
    String uri = "sip:" + username + "@" + ip + ":" + port;
    try
    {
      call.makeCall(uri, prm);
    } catch (Exception e)
    {
      call.delete();
      return null;
    }
    return call;
  }

  public void logout()
  {
    mAccountConfig.delete();
    mAccount.delete();
  }
}
