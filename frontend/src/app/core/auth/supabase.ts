import { InjectionToken, Provider } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { environment } from '../../../environments/environment';

export const SUPABASE_CLIENT = new InjectionToken<SupabaseClient>('SUPABASE_CLIENT');

export function provideSupabase(): Provider {
  return {
    provide: SUPABASE_CLIENT,
    useFactory: () => createClient(environment.supabaseUrl, environment.supabaseAnonKey),
  };
}
