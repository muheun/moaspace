'use client';

import { useState } from 'react';
import { Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import type { PostSearchField } from '@/types/api/post';

interface PostSearchFilterProps {
  onSearch: (query: string, fields?: PostSearchField[]) => void;
  isLoading?: boolean;
}

const FIELD_OPTIONS: Array<{ value: PostSearchField; label: string }> = [
  { value: 'title', label: '제목' },
  { value: 'content', label: '내용' },
  { value: 'hashtags', label: '해시태그' },
  { value: 'author', label: '작성자' },
];

export function PostSearchFilter({ onSearch, isLoading = false }: PostSearchFilterProps) {
  const [query, setQuery] = useState('');
  const [selectedFields, setSelectedFields] = useState<PostSearchField[]>([]);
  const [isAllFields, setIsAllFields] = useState(true);

  const handleFieldToggle = (field: PostSearchField, checked: boolean) => {
    if (checked) {
      setSelectedFields((prev) => [...prev, field]);
      setIsAllFields(false);
    } else {
      setSelectedFields((prev) => prev.filter((f) => f !== field));
      if (selectedFields.length === 1) {
        setIsAllFields(true);
      }
    }
  };

  const handleAllFieldsToggle = (checked: boolean) => {
    setIsAllFields(checked);
    if (checked) {
      setSelectedFields([]);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    const fields = isAllFields ? undefined : selectedFields;
    onSearch(query, fields);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* 검색어 입력 */}
      <div className="flex gap-2">
        <Input
          type="text"
          placeholder="검색어를 입력하세요..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          disabled={isLoading}
          className="flex-1"
        />
        <Button type="submit" disabled={isLoading || !query.trim()}>
          <Search className="w-4 h-4 mr-2" />
          검색
        </Button>
      </div>

      {/* 필드 필터 */}
      <div className="space-y-3">
        <Label className="text-sm font-medium">검색 범위</Label>
        <div className="flex flex-wrap gap-4">
          {/* 전체 검색 옵션 */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="field-all"
              checked={isAllFields}
              onCheckedChange={handleAllFieldsToggle}
              disabled={isLoading}
            />
            <Label
              htmlFor="field-all"
              className="text-sm font-normal cursor-pointer"
            >
              전체 검색
            </Label>
          </div>

          {/* 개별 필드 옵션 */}
          {FIELD_OPTIONS.map((option) => (
            <div key={option.value} className="flex items-center space-x-2">
              <Checkbox
                id={`field-${option.value}`}
                checked={!isAllFields && selectedFields.includes(option.value)}
                onCheckedChange={(checked: boolean) =>
                  handleFieldToggle(option.value, checked)
                }
                disabled={isLoading || isAllFields}
              />
              <Label
                htmlFor={`field-${option.value}`}
                className="text-sm font-normal cursor-pointer"
              >
                {option.label}만
              </Label>
            </div>
          ))}
        </div>
      </div>

      {/* 선택된 필드 요약 */}
      {!isAllFields && selectedFields.length > 0 && (
        <p className="text-sm text-muted-foreground">
          선택된 필드: {selectedFields.map((f) => FIELD_OPTIONS.find((o) => o.value === f)?.label).join(', ')}
        </p>
      )}
    </form>
  );
}
