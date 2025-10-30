/**
 * Footer 컴포넌트
 *
 * Constitution Principle X: semantic HTML 사용
 */
export function Footer() {
  return (
    <footer className="border-t mt-auto">
      <div className="container mx-auto px-4 py-6 text-center text-sm text-gray-600">
        <p>&copy; 2025 Vector AI Board. All rights reserved.</p>
        <p className="mt-2">
          Built with Next.js 15, React 19, and Spring Boot 3.2.1
        </p>
      </div>
    </footer>
  );
}
