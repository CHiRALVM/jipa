package org.chiralvm.libraries.NPAFormat;

import java.io.UnsupportedEncodingException;

public class EncryptionManager {
	private int k1=0,k2=0;
	private EncryptionKey encryptionKey;
	
	public EncryptionManager(EncryptionKey encryptionKey,int key1,int key2) {
		this.k1=key1;
		this.k2=key2;
		this.encryptionKey=encryptionKey;
	}
	
	/*
	 * Header Encryption Algorithm
	 * 
	 * It's symmetric:
	 * 
	 * to decrypt: add key and byte
	 * to encrypt: subtract key and byte
	 */
	
	public int hCrypt(int num,int file) {
		int key = 0xFC*num;
		int tmp = k1*k2;
		
		key -= tmp >> 0x18;
		key -= tmp >> 0x10;
		key -= tmp >> 0x08;
		key -= tmp & 0xff;
		
		key -= file >> 0x18;
		key -= file >> 0x10;
		key -= file >> 0x08;
		key -= file;
		
		return key & 0xff;
	}

	/*
	 * File Encryption Algorithm
	 * (Probably symmetric too)
	 * 
	 *
	 * (Integer overflow is intended and required)
	 */
	
	public int fCrypt(NPAEntry entry) {
		int key1 = 0;
		int key2= k1*k2;
		int key=0;
		int i=0; 
		
		if (encryptionKey.equals(EncryptionKey.DramaticalMurder) || 
				encryptionKey.equals(EncryptionKey.Kikokugai) ||
				encryptionKey.equals(EncryptionKey.Sonicomi_retail) ||
				encryptionKey.equals(EncryptionKey.Sonicomi_trial2) ||
				encryptionKey.equals(EncryptionKey.GuiltyCrown_trailer) ||
				encryptionKey.equals(EncryptionKey.AxanaelPlus_trial)) {
			key1 = 0x20101118;
		} else {
			key1 = 0x87654321;
		}
		
		try {
			byte[] orgName = entry.getOriginalName().getBytes("SJIS");
			for (i = 0;i < orgName.length;i++) {
				key1 -= orgName[i];
			}
		} catch (UnsupportedEncodingException ex) {}
		
		key = key1 * i;
		
		if (!encryptionKey.equals(EncryptionKey.Lamento)) {
			key += key2;
			key *= entry.getSize();
		}
				
		return key&0xff;
	}
}
