package com.projetofit.comum.supabase;

import java.util.UUID;

/** Operações administrativas no Supabase Auth. */
public interface SupabaseAdmin {

    /** Envia o convite por e-mail e devolve o id do usuário criado no Auth. */
    UUID convidar(String email, String nome);

    void excluirUsuario(UUID id);
}
