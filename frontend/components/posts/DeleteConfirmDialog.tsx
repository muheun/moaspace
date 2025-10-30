'use client';

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

/**
 * 게시글 삭제 확인 다이얼로그
 *
 * Constitution Principle VI: shadcn/ui AlertDialog 컴포넌트 활용
 * Constitution Principle X: ARIA 레이블 및 Semantic HTML 구현
 */
interface DeleteConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isDeleting?: boolean;
  postTitle?: string;
}

export function DeleteConfirmDialog({
  open,
  onOpenChange,
  onConfirm,
  isDeleting = false,
  postTitle,
}: DeleteConfirmDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>게시글 삭제 확인</AlertDialogTitle>
          <AlertDialogDescription>
            {postTitle ? (
              <>
                &ldquo;<strong>{postTitle}</strong>&rdquo; 게시글을 삭제하시겠습니까?
              </>
            ) : (
              '이 게시글을 삭제하시겠습니까?'
            )}
            <br />
            <span className="text-sm text-gray-500 mt-2 block">
              삭제된 게시글은 목록에서 숨겨지며, 관리자 도구를 통해 복구할 수 있습니다.
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isDeleting}>취소</AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={isDeleting}
            className="bg-red-600 hover:bg-red-700 focus:ring-red-600"
          >
            {isDeleting ? '삭제 중...' : '삭제'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
