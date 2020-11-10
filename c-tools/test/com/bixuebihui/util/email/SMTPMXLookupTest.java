package com.bixuebihui.util.email;

import org.junit.Test;

import static com.bixuebihui.util.email.SMTPMXLookup.isAddressValid;

public class SMTPMXLookupTest {

    @Test
   public void testIsAddressValid( ) {
             String testData[] = {
                     /*
         "real@rgagnon.com",
         "you@acquisto.net",
         "fail.me@nowhere.spam", // Invalid domain name
         "arkham@bigmeanogre.net", // Invalid address
         "nosuchaddress@yahoo.com" // Failure of this method
         */
                         "131212312312312@msn.com",
                                 "xwx@live.cn",
                                 "www@qsn.so",
                                 "xwx@youth.cn",
                             };

                     for ( int ctr = 0 ; ctr < testData.length ; ctr++ ) {
                    System.out.println( testData[ ctr ] + " is valid? " +
                                  isAddressValid( testData[ ctr ] ) );
                 }
             return;
             }

}