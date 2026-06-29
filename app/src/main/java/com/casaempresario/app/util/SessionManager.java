package com.casaempresario.app.util;

import android.content.Context;
import android.content.SharedPreferences;

// verificar segurança do armazenamento de tokens (criptografia) - Mateus (20/05)
public class SessionManager {

    private static final String PREF_NAME = "CasaEmpresarioSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_NOME = "nome";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_PROFILE_PHOTO_URI = "profilePhotoUri";
    private static final String KEY_EMPRESA = "empresa";
    private static final String KEY_CARGO = "cargo";
    private static final String KEY_CIDADE = "cidade";
    private static final String KEY_TELEFONE = "telefone";
    private static final String KEY_LINKEDIN = "linkedin";
    private static final String KEY_BIO = "bio";

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
        editor.putString(KEY_PROFILE_PHOTO_URI, "");
        editor.apply();
    }

    public void salvarVisitante() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_TOKEN);
        editor.putString(KEY_NOME, "Visitante");
        editor.putString(KEY_EMAIL, "");
        editor.putString(KEY_ROLE, "VISITANTE");
        editor.putLong(KEY_USER_ID, -1);
        editor.apply();
    }

    public boolean isLogado() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public boolean isVisitante() {
        return "VISITANTE".equalsIgnoreCase(getRole()) || !isLogado();
    }

    public String getToken() {
        return "Bearer " + prefs.getString(KEY_TOKEN, "");
    }

    public String getNome() {
        return prefs.getString(KEY_NOME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "PARTICIPANTE");
    }

    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getProfilePhotoUri() {
        return prefs.getString(KEY_PROFILE_PHOTO_URI, "");
    }

    public void setProfilePhotoUri(String profilePhotoUri) {
        prefs.edit().putString(KEY_PROFILE_PHOTO_URI, safe(profilePhotoUri)).apply();
    }

    public void salvarPerfilProfissional(String empresa, String cargo, String cidade, String telefone, String linkedin, String bio) {
        prefs.edit()
                .putString(KEY_EMPRESA, safe(empresa))
                .putString(KEY_CARGO, safe(cargo))
                .putString(KEY_CIDADE, safe(cidade))
                .putString(KEY_TELEFONE, safe(telefone))
                .putString(KEY_LINKEDIN, safe(linkedin))
                .putString(KEY_BIO, safe(bio))
                .apply();
    }

    public String getEmpresa() {
        return prefs.getString(KEY_EMPRESA, "");
    }

    public String getCargo() {
        return prefs.getString(KEY_CARGO, "");
    }

    public String getCidade() {
        return prefs.getString(KEY_CIDADE, "");
    }

    public String getTelefone() {
        return prefs.getString(KEY_TELEFONE, "");
    }

    public String getLinkedin() {
        return prefs.getString(KEY_LINKEDIN, "");
    }

    public String getBio() {
        return prefs.getString(KEY_BIO, "");
    }

    public int getPercentualPerfilCompleto() {
        int total = 7;
        int preenchidos = 0;
        if (!isBlank(getNome())) preenchidos++;
        if (!isBlank(getEmail())) preenchidos++;
        if (!isBlank(getEmpresa())) preenchidos++;
        if (!isBlank(getCargo())) preenchidos++;
        if (!isBlank(getCidade())) preenchidos++;
        if (!isBlank(getTelefone())) preenchidos++;
        if (!isBlank(getBio())) preenchidos++;
        return Math.round((preenchidos * 100f) / total);
    }

    public boolean isOrganizador() {
        return isLogado() && "ORGANIZADOR".equalsIgnoreCase(getRole());
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    private String safe(String valor) {
        return valor != null ? valor : "";
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
