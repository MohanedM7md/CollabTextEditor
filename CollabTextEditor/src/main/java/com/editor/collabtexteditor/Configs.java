package com.editor.collabtexteditor;

import java.util.UUID;

public class Configs {
        public static final String BASE_URL = "http://localhost:8080/";
        public static final String API_URL = "http://localhost:8080/api/";
        public static final String generatedUserId = UUID.randomUUID().toString().substring(0, 4);
}
