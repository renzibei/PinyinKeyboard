package com.shiweinan.keyboard;

abstract class Constants {
    static final String serverAddr = "DC:A6:32:45:64:DC";
    static final String uuidString = "00001101-0000-1000-8000-00805f9b34fb";

    static final int listenPort = 13972;

    static final int MSG_DATA_LEN = 256;

    static final int MAX_WORD_LEN = 14;

    static final int MAX_NET_SEN_LEN = 100;
    static final int MAX_NET_WORD_LEN = 20;

    static final String infoTag = "pinyin_info";
    static final String debugTag = "pinyin_debug";
    static final String errorTag = "pinyin_error";

    static final String wordTextFilePath = "configs/word_texts.txt";
    static final String frequencyFilePath = "configs/frequency.txt";
    static final String centerPosFilePath = "configs/frequency.txt";

    static final double M_PI = 3.141592654;


}