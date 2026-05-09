package com.casaempresario.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "CasaEmpresarioSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_NOME = "nome";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void salvarSessao(String token, String nome, String email, String role, Long userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_NOME, nome);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);
        editor.putLong(KEY_USER_ID, userId != null ? userId : -1);
        editor.apply();
    }

    public boolean isLogado() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public String getToken() {
        return "Bearer " + prefs.getString(KEY_TOKEN, "");
    }

    public String getNome() { return prefs.getString(KEY_NOME, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, "USER"); }
    public Long getUserId() { return prefs.getLong(KEY_USER_ID, -1); }

    public boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
