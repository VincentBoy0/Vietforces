export default function UnauthorizedPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-muted">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-danger mb-2">
          Không có quyền truy cập
        </h1>
        <p className="text-muted-foreground text-sm">
          Bạn không có quyền admin để truy cập trang này.
        </p>
        <a
          href="/login"
          className="mt-4 inline-block text-primary text-sm hover:underline"
        >
          Quay lại trang đăng nhập
        </a>
      </div>
    </div>
  )
}
