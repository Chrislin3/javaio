package aio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;
//AIO客户端代码，负责连接服务器，声明通道，连接通道
public class ITDragonAIOClient implements Runnable{
    private static Integer PORT = 8888;
    private static String IP_ADDRESS = "127.0.0.1";
    private AsynchronousSocketChannel asynSocketChannel ;
    public ITDragonAIOClient() throws Exception {
        asynSocketChannel = AsynchronousSocketChannel.open();  // 打开通道
    }
    public void connect(){
        asynSocketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));  // 创建连接 和NIO一样
    }

    public void write(String request){
        try {
            asynSocketChannel.write(ByteBuffer.wrap(request.getBytes())).get();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            asynSocketChannel.read(byteBuffer).get();
            byteBuffer.flip();
            byte[] respByte = new byte[byteBuffer.remaining()];
            byteBuffer.get(respByte); // 将缓冲区的数据放入到 byte数组中
            System.out.println(new String(respByte,"utf-8").trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        while(true){
        }
    }
    public static void main(String[] args) throws Exception{
        for (int i = 0; i < 10; i++) {
            ITDragonAIOClient aioClient = new ITDragonAIOClient();
            aioClient.connect();
      //      new Thread(aioClient, "aioClient").start();
            Random random = new Random(System.currentTimeMillis());
            String expression = String.valueOf(random.nextInt(10)+(random.nextInt(10)+1));
            aioClient.write(expression);
        }
    }
}
