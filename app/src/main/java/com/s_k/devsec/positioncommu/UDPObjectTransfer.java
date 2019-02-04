package com.s_k.devsec.positioncommu;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class UDPObjectTransfer {
    /**
     * オブジェクトを送信する。
     *
     * @param map     オブジェクト
     * @param address 宛先アドレス。192.168.1.255のようにネットワークアドレスを指定するとブロードキャスト送信。
     * @param port    宛先ポート。受信側と揃える。
     * @throws IOException
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void send(Map<String, String> map, String address, int port) throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress IPAddress = InetAddress.getByName(address);
            byte[] sendData = convertToBytes(map);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            clientSocket.send(sendPacket);
        }
    }

    /**
     * オブジェクトをバイト配列に変換する。
     *
     * @param  object Serializableを実装していなければいけない。
     * @return バイト配列
     * @throws IOException シリアライズに失敗した時に発生する
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }


}
