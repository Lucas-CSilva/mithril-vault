import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const AUTH_PATHS = ["/login", "/register"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = request.cookies.has("refreshToken");
  const isAuthPath = AUTH_PATHS.some((path) => pathname.startsWith(path));

  if (isAuthPath && hasSession) {
    return NextResponse.redirect(new URL("/", request.url));
  }
  if (!isAuthPath && !hasSession) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/).*)"],
};
