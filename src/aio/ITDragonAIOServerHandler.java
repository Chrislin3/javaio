package aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
//AIO服务器任务处理代码，负责，读取数据，写入数据
public class ITDragonAIOServerHandler implements CompletionHandler<AsynchronousSocketChannel, ITDragonAIOServer> {
    private final Integer BUFFER_SIZE = 1024;
    @Override
    public void completed(AsynchronousSocketChannel asynSocketChannel, ITDragonAIOServer attachment) {
        // 保证多个客户端都可以阻塞
        attachment.asynServerSocketChannel.accept(attachment, this);
        read(asynSocketChannel);
    }
    //读取数据
    private void read(final AsynchronousSocketChannel asynSocketChannel) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        asynSocketChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer resultSize, ByteBuffer attachment) {
                //进行读取之后,重置标识位
                attachment.flip();
                //获取读取的数据
                String resultData = new String(attachment.array()).trim();
                System.out.println("Server -> " + "收到客户端的数据信息为:" + resultData);

                write(asynSocketChannel, resultData);
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
            }
        });
    }
    // 写入数据
    private void write(AsynchronousSocketChannel asynSocketChannel, String response) {
        try {
            // 把数据写入到缓冲区中
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
            buf.put(response.getBytes());
            buf.flip();
            // 在从缓冲区写入到通道中
            asynSocketChannel.write(buf).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Throwable exc, ITDragonAIOServer attachment) {
        exc.printStackTrace();
    }
}
