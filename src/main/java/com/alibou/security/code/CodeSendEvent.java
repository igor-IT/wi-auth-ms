package com.alibou.security.code;

import com.alibou.security.user.Locale;

public record CodeSendEvent(String code, Locale locale) {
}
