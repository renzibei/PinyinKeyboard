package com.shiweinan.keyboard;


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class TcpSocketServer {
    static void startListen() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket tcpServer = null;
                boolean succFlag = false;
                try {
                    tcpServer = new ServerSocket(Constants.listenPort, 100);
                    succFlag = true;
                }
                catch (IOException e) {
                    Log.e(Constants.errorTag, "Tcp server fail to bind, " + e.getMessage());
                }
                if (succFlag) {
                    while (true) {
                        BufferedInputStream tcpInputStream;
                        BufferedOutputStream tcpOutputStream;
                        try {
                            Socket tcpSocket = tcpServer.accept();
                            tcpInputStream = new BufferedInputStream(tcpSocket.getInputStream());
                            tcpOutputStream = new BufferedOutputStream(tcpSocket.getOutputStream());
                        } catch (IOException e1) {
                            Log.e(Constants.errorTag, "Fail to accept a connect, " + e1.getMessage());
                            continue;
                        }
                        try {
                            warmup(tcpInputStream, tcpOutputStream);
                        }
                        catch (IOException e3) {
                            Log.e(Constants.errorTag, "Fail to handshake the tcp connection, " + e3.getMessage());
                            continue;
                        }
                        while (true) {
                            try {
                                NetMsgStruct msgStruct = readMsg(tcpInputStream);
                                if (msgStruct.msgType == 101) {
                                    recvCandidateWords(msgStruct.msgLen, msgStruct.msg);
                                }
                                else if(msgStruct.msgType == 102) {
                                    recvDownSen(msgStruct.msgLen, msgStruct.msg);
                                }
                                else if (msgStruct.msgType == 103) {
                                    recvUpSen(msgStruct.msgLen, msgStruct.msg);
                                }
                                else if (msgStruct.msgType == 106) {
                                    recvHighlightIndex(msgStruct.msgLen, msgStruct.msg);
                                }
                                else if (msgStruct.msgType == 107) {
                                    // commit one word
                                    recvCommitWord(msgStruct.msgLen, msgStruct.msg);
//                                        Log.d(Constants.debugTag, "commit word: " + )
                                }
                                else if (msgStruct.msgType == 108) {
                                    recvCharsDeleted(msgStruct.msgLen, msgStruct.msg);
                                }
                            }
                            catch (IOException e2) {
                                Log.e(Constants.errorTag, "Fail to receive msg " + e2.getMessage());
                                break;
                            }

                        }
//                                testRecv(tcpInputStream);
//                                testSend(tcpOutputStream);

                    }
                }
            }
        }).start();


    }

    private static void recvHighlightIndex(int msgLen, byte[] indexMsg) {
        assert(msgLen == 4);
        assert(indexMsg.length == 4);
        ByteBuffer byteBuffer = ByteBuffer.wrap(indexMsg);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int index = byteBuffer.getInt();
        ByteBuffer otherBuffer = ByteBuffer.wrap(indexMsg);
        otherBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int otherOrderIndex = otherBuffer.getInt();
        Log.d(Constants.debugTag, "set highlight index: " + index  + " other-order: " + otherOrderIndex);
        IMEService.setHighlightWord(index);
//        MainActivity.setHighlightWord(index);
    }

    private static void recvCharsDeleted(int msgLen, byte[] msg) {
        assert(msgLen == 4);
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int deletedCharNum = byteBuffer.getInt();
        IMEService.deleteInputChars(deletedCharNum);
//        IMEService.setHighlightWord(index);
    }

    private static void recvCommitWord(int msgLen, byte[] msgData) {
        assert(msgLen < Constants.MAX_NET_WORD_LEN);
        String word =  new String(msgData, StandardCharsets.UTF_8);
        IMEService.addCommitWord(word);
//        MainActivity.addCommitWord(word);;
    }



    private static void recvCandidateWords(int msgLen, byte[] wordsData) {
        assert(msgLen == Constants.MAX_NET_WORD_LEN * 5);
        String[] strArr = new String[5];
        for (int i = 0; i < 5; ++i) {
            byte[] tempBytes = Arrays.copyOfRange(wordsData, i * Constants.MAX_NET_WORD_LEN, (i+1) * Constants.MAX_NET_WORD_LEN);
            int tempWordLen = 0;
            for (int j = 0; j < Constants.MAX_NET_WORD_LEN; ++j) {
                if (tempBytes[j] == 0) {
                    tempWordLen = j;
                    break;
                }
            }
            byte[] wordBytes = Arrays.copyOf(tempBytes, tempWordLen);
            strArr[i] = new String(wordBytes, StandardCharsets.UTF_8);
        }
        IMEService.updateHintWords(strArr);
//        MainActivity.updateHintWords(strArr);
    }

    private static void recvDownSen(int msgLen, byte[] msg) {
        assert(msgLen <= Constants.MAX_NET_SEN_LEN);
        String downSen = new String(msg, StandardCharsets.UTF_8);
//        MainActivity.updateDownSentence(downSen);
    }

    private static void recvUpSen(int msgLen, byte[] msg) {
        assert(msgLen <= Constants.MAX_NET_SEN_LEN);
        String upSen = new String(msg, StandardCharsets.UTF_8);
//        MainActivity.updateUpSentence(upSen);
    }

    private static void warmup(BufferedInputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        Log.d(Constants.debugTag, "Begin host tcp warmup");
        int x = readMsgInt(inputStream);
        assert(x == 101);
        x = 102;
        writeMsgInt(outputStream, 105, x);
        Log.d(Constants.debugTag, "After host tcp warmup");
    }

    static class NetMsgStruct{
        public int msgLen;
        public int msgType;
        byte[] msg;
    };

    private static NetMsgStruct readMsg(BufferedInputStream inputStream) throws IOException {
        int packetLen = readBeginLength(inputStream);
        int msgType = readBeginLength(inputStream);
        Log.d(Constants.debugTag, "readMsg packetLen: " + packetLen);
        int msgLen = packetLen - 4;
        NetMsgStruct retStruct = new NetMsgStruct();
        retStruct.msg = new byte[msgLen];
        try {
            if (msgLen == 4) {
                for (int i = 0; i < 4; ++i) {
                    retStruct.msg[i] = 0;
                }
            }
            int readLen = inputStream.read(retStruct.msg, 0, msgLen);
            assert(readLen == msgLen);
        }
        catch (IOException e) {
            Log.e(Constants.errorTag, "Fail to readBytes, " + e.getMessage());
            throw e;
        }

        retStruct.msgLen = msgLen;
        retStruct.msgType = msgType;
        Log.d(Constants.debugTag, "readMsg, msgLen: " + msgLen + " msgType " + msgType);
        if (msgType == 106) {
            int[] tempArray = new int[4];
            for(int i = 0; i < 4; ++i) {
                tempArray[i] = retStruct.msg[i];
            }
            Log.d(Constants.debugTag, " raw index data: " + tempArray[0] + " " + tempArray[1] + " " + tempArray[2] + " " + tempArray[3]);
        }
        return retStruct;
    }

    private static void writeMsg(BufferedOutputStream outputStream, int msgLen, int msgType, byte[] data) {
        int sendLen = msgLen + 4;
        writeBeginLength(sendLen, outputStream);
        writeBeginLength(msgType, outputStream);
        try {
            outputStream.write(data);
            outputStream.flush();
        }
        catch (IOException e) {
            Log.e(Constants.errorTag, "Fail to write bytes to tcp, " + e.getMessage());
        }
    }

    private static void testRecv(BufferedInputStream inputStream) throws IOException{
        Log.i(Constants.infoTag, "Prepare to recv");
        int x = readInt(inputStream);
        Log.i(Constants.infoTag, "Recv " + x + " from client");
    }

    private static void testSend(BufferedOutputStream outputStream) {
        int x = 102;
        Log.i(Constants.infoTag, "Prepare to send " + x);
        writeInt(x, outputStream);
        Log.i(Constants.infoTag, "Sent " + x);
    }

    private static int readBeginLength(BufferedInputStream inputStream) throws IOException {
        byte[] dataByte = new byte[4];
        try {
            int realLen =  inputStream.read(dataByte, 0, 4);
            if(realLen < 4) {
                Log.e(Constants.errorTag, "read realLen < 4, realLen = " + realLen);
                throw new IOException("read realLen < 4, realLen = " + realLen);
            }
        } catch(IOException e) {
            Log.e(Constants.errorTag, "Fail to read begin length");
            Log.e(Constants.errorTag, e.getMessage(), e);
            throw e;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(dataByte);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer.getInt();
    }

    private static int readInt(BufferedInputStream inputStream) throws IOException {
        int beginLen = readBeginLength(inputStream);
        if(beginLen != 4) {
            Log.e(Constants.errorTag, "read begin len for int not equals 4");
            return -1;
        }
        return readBeginLength(inputStream);
    }

    private static int readMsgInt(BufferedInputStream inputStream) throws IOException {
        NetMsgStruct msgStruct = readMsg(inputStream);
        assert(msgStruct.msgLen == 4);
        ByteBuffer byteBuffer = ByteBuffer.wrap(msgStruct.msg);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer.getInt();
    }

    private static void writeMsgInt(BufferedOutputStream outputStream, int msgType, int intData) {
        writeBeginLength(8, outputStream);
        writeBeginLength(msgType, outputStream);
        writeBeginLength(intData, outputStream);
    }

    private static void writeBeginLength(int len, BufferedOutputStream outputStream) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.putInt(len);
        try {
            outputStream.write(byteBuffer.array());
            outputStream.flush();
        } catch (IOException e) {
            Log.e(Constants.errorTag, "Fail to write begin length");
            Log.e(Constants.errorTag, e.getMessage(), e);
        }
    }

    private static void writeInt(int intData, BufferedOutputStream outputStream) {
        writeBeginLength(4, outputStream);
        writeBeginLength(intData, outputStream);
    }
}
