'use server';

import { cookies } from 'next/headers';

/**
 * JWT 토큰을 Cookie에 저장하는 Server Action
 *
 * Middleware에서 인증 상태를 확인할 수 있도록 Cookie에 저장
 */
export async function setAuthCookie(token: string) {
  const cookieStore = await cookies();

  cookieStore.set('access_token', token, {
    httpOnly: false, // localStorage와 동기화를 위해 client에서도 접근 가능
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 7, // 7일
    path: '/',
  });
}
