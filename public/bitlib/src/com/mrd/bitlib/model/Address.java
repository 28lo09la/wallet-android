/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.bitcoinj.Base58;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;

public class Address implements Serializable, Comparable<Address> {

   private static final long serialVersionUID = 1L;
   public static final int NUM_ADDRESS_BYTES = 21;
   private byte[] _bytes;
   private String _address;

   public static Collection<Address> fromStrings(Collection<String> addresses, NetworkParameters network) {
      List<Address> list = new LinkedList<Address>();
      for (String address : addresses) {
         Address a = Address.fromString(address, network);
         if (a == null) {
            return null;
         }
         list.add(a);
      }
      return list;
   }

   public static Address[] fromStrings(String[] addressStrings) {
      Address[] addresses = new Address[addressStrings.length];
      for (int i = 0; i < addressStrings.length; i++) {
         addresses[i] = Address.fromString(addressStrings[i]);
      }
      return addresses;
   }

   public static String[] toStrings(Address[] addresses) {
      String[] addressStrings = new String[addresses.length];
      for (int i = 0; i < addressStrings.length; i++) {
         addressStrings[i] = addresses[i].toString();
      }
      return addressStrings;
   }

   public static Address fromString(String address, NetworkParameters network) {
      Address addr = Address.fromString(address);
      if (addr == null) {
         return null;
      }
      int version = addr.getVersion();
      if (version != network.getStandardAddressHeader() && version != network.getMultisigAddressHeader()) {
         return null;
      }
      return addr;
   }

   public static Address fromString(String address) {
      if (address == null) {
         return null;
      }
      if (address.length() == 0) {
         return null;
      }
      byte[] bytes = Base58.decodeChecked(address);
      if (bytes == null || bytes.length != NUM_ADDRESS_BYTES) {
         return null;
      }
      return new Address(bytes);
   }

   public static Address fromMultisigBytes(byte[] bytes, NetworkParameters network) {
      if (bytes.length != 20) {
         return null;
      }
      byte[] all = new byte[NUM_ADDRESS_BYTES];
      all[0] = (byte) (network.getMultisigAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, all, 1, 20);
      return new Address(all);
   }

   public static Address fromStandardBytes(byte[] bytes, NetworkParameters network) {
      if (bytes.length != 20) {
         return null;
      }
      byte[] all = new byte[NUM_ADDRESS_BYTES];
      all[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, all, 1, 20);
      return new Address(all);
   }

   public static Address fromStandardPublicKey(PublicKey key, NetworkParameters network) {
      byte[] hashedPublicKey = HashUtils.addressHash(key.getPublicKeyBytes());
      byte[] addressBytes = new byte[1 + 20];
      addressBytes[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      System.arraycopy(hashedPublicKey, 0, addressBytes, 1, 20);
      return new Address(addressBytes);
   }

   /**
    * Construct a Bitcoin address from an array of bytes containing both the
    * address version and address bytes, but without the checksum (1 + 20 = 21
    * bytes).
    * 
    * @param bytes
    *           containing the full address representation 1 + 20 bytes.
    */
   public Address(byte[] bytes) {
      _bytes = bytes;
      _address = null;
   }

   /**
    * Construct a Bitcoin address from an array of bytes and the string
    * representation of the address. The byte array contains both the address
    * version and address bytes, but without the checksum (1 + 20 = 21 bytes).
    * 
    * Note: No attempt is made to verify that the byte array and string
    * representation match.
    * 
    * @param bytes
    *           containing the full address representation 1 + 20 bytes.
    * @param stringAddress
    *           the string representation of a Bitcoin address
    */
   public Address(byte[] bytes, String stringAddress) {
      _bytes = bytes;
      _address = stringAddress;
   }

   public String getThreeLines() {
      String address = toString();
      String one = address.substring(0, 12);
      String two = address.substring(12, 24);
      String three = address.substring(24);
      address = one + "\r\n" + two + "\r\n" + three;
      return address;
   }

   /**
    * Validate that an address is a valid address on the specified network
    */
   public boolean isValidAddress(NetworkParameters network) {
      byte version = getVersion();
      if (getAllAddressBytes().length != NUM_ADDRESS_BYTES) {
         return false;
      }
      return ((byte) (network.getStandardAddressHeader() & 0xFF)) == version
            || ((byte) (network.getMultisigAddressHeader() & 0xFF)) == version;
   }

   public boolean isMultisig(NetworkParameters network) {
      return getVersion() == (byte) (network.getMultisigAddressHeader() & 0xFF);
   }

   public byte getVersion() {
      return _bytes[0];
   }

   /**
    * Get the address as an array of bytes. The array contains the one byte
    * address type and the 20 address bytes, totaling 21 bytes.
    * 
    * @return The address as an array of 21 bytes.
    */
   public byte[] getAllAddressBytes() {
      return _bytes;
   }

   public byte[] getTypeSpecificBytes() {
      byte[] result = new byte[20];
      System.arraycopy(_bytes, 1, result, 0, 20);
      return result;
   }

   @Override
   public String toString() {
      if (_address == null) {
         byte[] addressBytes = new byte[1 + 20 + 4];
         addressBytes[0] = _bytes[0];
         System.arraycopy(_bytes, 0, addressBytes, 0, NUM_ADDRESS_BYTES);
         byte[] checkSum = HashUtils.doubleSha256(addressBytes, 0, NUM_ADDRESS_BYTES);
         System.arraycopy(checkSum, 0, addressBytes, NUM_ADDRESS_BYTES, 4);
         _address = Base58.encode(addressBytes);
      }
      return _address;
   }

   @Override
   public int hashCode() {
      return ((_bytes[16] & 0xFF) << 0) | ((_bytes[17] & 0xFF) << 8) | ((_bytes[18] & 0xFF) << 16)
            | ((_bytes[19] & 0xFF) << 24);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Address)) {
         return false;
      }
      return BitUtils.areEqual(_bytes, ((Address) obj)._bytes);
   }

   public static Address getNullAddress(NetworkParameters network) {
      byte[] bytes = new byte[NUM_ADDRESS_BYTES];
      bytes[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      return new Address(bytes);
   }

   @Override
   public int compareTo(Address other) {

      // We sort on the actual address bytes.
      // We wish to achieve consistent sorting, the exact order is not
      // important.
      for (int i = 0; i < NUM_ADDRESS_BYTES; i++) {
         byte a = _bytes[i];
         byte b = other._bytes[i];
         if (a < b) {
            return -1;
         } else if (a > b) {
            return 1;
         } else {
            continue;
         }
      }
      return 0;
   }

}
