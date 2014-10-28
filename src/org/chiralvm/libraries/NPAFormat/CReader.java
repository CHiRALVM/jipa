package org.chiralvm.libraries.NPAFormat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CReader {
	private DataInputStream dis;
	private long ptr=0;
	public CReader(InputStream is) {
		dis = new DataInputStream(is);
	}
	
	public long getPosition() {
		return ptr;
	}
	
	public void close(){ try { dis.close(); } catch (IOException ex) {}}
	
	public byte readWord() throws IOException {
		byte[] buffer = new byte[1];
		dis.read(buffer,0,1);
		ptr += buffer.length;
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).array()[0];
	}

	public int readDWord() throws IOException {
		byte[] buffer = new byte[4];
		dis.read(buffer,0,4);
		ptr += buffer.length;
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public int readQWord() throws IOException {
		byte[] buffer = new byte[8];
		dis.read(buffer,0,8);
		ptr += buffer.length;
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public void skip(long bytes) {
		try {
		dis.skip(bytes);
		} catch (IOException io) {
			System.out.println("Error: Unexcepted EOF");
		}
		ptr += bytes;
	}
	
	public byte[] readBytes(int len) throws IOException {
		byte[] buffer = new byte[len];
		dis.read(buffer,0,len);
		ptr += buffer.length;
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).array();
	}
	
	public byte[] readBytesRaw(int len) throws IOException {
		byte[] buffer = new byte[len];
		dis.read(buffer,0,len);
		ptr += buffer.length;
		return buffer;
	}
	
	public int[] readUnsignedBytes(int len) throws IOException {
		int[] buffer = new int[len];
		for (int i = 0;i < buffer.length;i++) {
			buffer[i] = dis.readUnsignedByte();
		}
		ptr += buffer.length;
		return buffer;
	}
}
