'use server';

import { cookies } from 'next/headers';

/**
 * 로그아웃 시 Cookie 삭제
 */
export async function clearAuthCookie() {
  const cookieStore = await cookies();
  cookieStore.delete('access_token');
}
