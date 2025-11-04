/**
 * Footer 컴포넌트
 *
 * Constitution Principle X: semantic HTML 사용
 * Web UI Design Guide: 16px minimum text size, ARIA attributes
 */
export function Footer() {
  return (
    <footer className="border-t mt-auto" role="contentinfo">
      <div className="container mx-auto px-4 py-6 text-center text-base text-muted-foreground">
        <p>&copy; 2025 Vector AI Board. All rights reserved.</p>
        <p className="mt-2">
          Built with Next.js 15, React 19, and Spring Boot 3.2.1
        </p>
      </div>
    </footer>
  );
}
