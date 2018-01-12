package com.skyline.wxjumphack;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * 微信游戏“跳一跳”，外挂！
 * Created by chenliang on 2018/1/1.
 */
public class Hack {


    static final String ADB_PATH = "/softDevelop/android-sdk-windows/platform-tools/adb";

    /**
     * 弹跳系数，现在已经会自动适应各种屏幕，请不要修改。
     */
    static final double JUMP_RATIO = 1.385f;
    
    public static void main(String... strings) {

    	/*
    	 * 获取当前类Hack的class文件的包所处的根路径地址!
    	 * 注：是包的根路径，而不是当前类的class文件的根路径地址。
    	 * （class文件的完整包路径与对应的类的完整包路径相同）
    	 * 如下：Hack类的完整包路径是：com\skyline\wxjumphack\Hack.javan，
    	 * 则其class文件的完整包路径是：com\skyline\wxjumphack\Hack.class
    	 * 代码输出："/D:/softDevelop/workspace/wechat-jump/bin/"
    	 */
        String root = Hack.class.getResource("/").getPath();
        System.out.println("root: " + root);
        File srcDir = new File(root, "imgs/input");
        srcDir.mkdirs();
        System.out.println("srcDir: " + srcDir.getAbsolutePath());
        MyPosFinder myPosFinder = new MyPosFinder();
        NextCenterFinder nextCenterFinder = new NextCenterFinder();
        WhitePointFinder whitePointFinder = new WhitePointFinder();
        int total = 0;
        int centerHit = 0;
        double jumpRatio = 0;
        for (int i = 0; i < 5000; i++) {
        	//时间戳
        	long time = System.currentTimeMillis();
        	
            try {
                total++;
                File file = new File(srcDir, i + ".png");
                if (file.exists()) {
                    file.deleteOnExit();
                }
                
                /*
                 * Runtime.getRuntime().exec():
                 * 在程序运行中，有时需要执行外部程序或命令，则可以通过上述方法实现。
                 * 注意：该方法会生成一个新的进程去执行外部程序或命令,异步执行新的进程和原来的进程！！
                 * 
                 */
                
                //执行adb命令，手机截图，并将图片存在手机中
                Process p = Runtime.getRuntime().exec(ADB_PATH + " shell /system/bin/screencap -p /sdcard/screenshot_"+time+".png");
                //读取外部进程标准输出流（清理标准输出流缓存）
                printMessage(p.getInputStream());
                //读取外部进程错误输出流（清理错误输出流缓存）
                printMessage(p.getErrorStream());
                /*
                 * 获取外部进程执行结束之后的结果。
                 * 注：waitFor()方法将会挂起当前进程，直到外部进程执行结束。（异步改同步）
                 */
                int result = p.waitFor();
                System.out.println("截图结果："+result);
//                Thread.sleep(1_000);
                //执行adb命令，将截取的图片从手机中复制到电脑指定位置。
                Process process = Runtime.getRuntime().exec(ADB_PATH + " pull /sdcard/screenshot_"+time+".png " + file.getAbsolutePath());
                printMessage(process.getInputStream());
                printMessage(process.getErrorStream());
                int value = process.waitFor();
                System.out.println("复制结果："+value);
//                Thread.sleep(1_000);

                System.out.println("screenshot, file: " + file.getAbsolutePath());
                //图片装载
                BufferedImage image = ImgLoader.load(file.getAbsolutePath());
                //获取弹跳系数
                if (jumpRatio == 0) {
                    jumpRatio = JUMP_RATIO * 1080 / image.getWidth();
                }
                //获取玩家位置坐标
                int[] myPos = myPosFinder.find(image);
                if (myPos != null) {
                    System.out.println("find myPos, succ, (" + myPos[0] + ", " + myPos[1] + ")");
                    //获取目标方块位置
                    int[] nextCenter = nextCenterFinder.find(image, myPos);
                    if (nextCenter == null || nextCenter[0] == 0) {
                        System.err.println("find nextCenter, fail");
                        break;
                    } else {
                        int centerX, centerY;
                        //获取目标方块中心白点的位置
                        int[] whitePoint = whitePointFinder.find(image, nextCenter[0] - 120, nextCenter[1], nextCenter[0] + 120, nextCenter[1] + 180);
                        if (whitePoint != null) {
                        	//如果能识别出目标方块中心的白点的位置，则将白点的坐标作为下一个跳跃目标
                            centerX = whitePoint[0];
                            centerY = whitePoint[1];
                            centerHit++;
                            System.out.println("find whitePoint, succ, (" + centerX + ", " + centerY + "), centerHit: " + centerHit+ ", total: " + total);
                        } else {
                        	//如果哦不能识别出目标方块中心的白点的位置，则将目标方块的中心作为下一个跳跃目标
                            if (nextCenter[2] != Integer.MAX_VALUE && nextCenter[4] != Integer.MIN_VALUE) {
                                centerX = (nextCenter[2] + nextCenter[4]) / 2;
                                centerY = (nextCenter[3] + nextCenter[5]) / 2;
                            } else {
                                centerX = nextCenter[0];
                                centerY = nextCenter[1] + 48;
                            }
                        }
                        System.out.println("find nextCenter, succ, (" + centerX + ", " + centerY + ")");
                        //算出长按时长
                        int distance = (int) (Math.sqrt((centerX - myPos[0]) * (centerX - myPos[0]) + (centerY - myPos[1]) * (centerY - myPos[1])) * jumpRatio);
                        System.out.println("distance: " + distance);
                        System.out.println(ADB_PATH + " shell input swipe 400 400 400 400 " + distance);
                        //通过adb命令触发长按
                        Process pro = Runtime.getRuntime().exec(ADB_PATH + " shell input swipe 300 300 400 400 " + distance);
                        printMessage(pro.getInputStream());
                        printMessage(pro.getErrorStream());
                        int value1 = pro.waitFor();
                        System.out.println("触发长按结果："+value1);
                    }
                } else {
                    System.err.println("find myPos, fail");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("centerHit: " + centerHit + ", total: " + total);
    }
    
    
    /**
     * 开启一个新的线程，读取输入流
     */
    private static void printMessage(final InputStream input) {
    	new Thread(new Runnable(){
    		public void run() {
    			Reader reader = new InputStreamReader(input);
    			BufferedReader bf = new BufferedReader(reader);
    			String line = null;
    			try {
					while((line=bf.readLine())!=null) {
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}).start();
    }

}
