import Link from 'next/link'
import Image from 'next/image'
import { listWords, listCategories } from '@/lib/actions/vocabulary'

export default async function VocabularyPage({
  searchParams,
}: {
  searchParams: Promise<{ category?: string; page?: string }>
}) {
  const params = await searchParams
  const category = params.category ?? 'all'
  const page = parseInt(params.page ?? '1', 10)

  const [{ words, total }, categories] = await Promise.all([
    listWords(category, page, 20),
    listCategories(),
  ])

  const totalPages = Math.ceil(total / 20) || 1

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Vocabulary</h1>
        <Link
          href="/admin/vocabulary/new"
          className="bg-primary text-white px-4 py-2 rounded hover:opacity-90 transition-opacity"
        >
          Add Word
        </Link>
      </div>

      {/* Filter bar */}
      <div className="mb-4 flex gap-3 items-center">
        <span className="text-sm font-medium">Category:</span>
        <form method="GET" action="/admin/vocabulary" className="flex gap-2 items-center">
          <select
            name="category"
            defaultValue={category}
            className="border border-border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="all">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <input type="hidden" name="page" value="1" />
          <button
            type="submit"
            className="bg-muted border border-border px-3 py-1.5 rounded text-sm hover:bg-muted/80 transition-colors"
          >
            Filter
          </button>
        </form>
        <span className="text-sm text-muted-foreground">{total} items</span>
      </div>

      {/* Table */}
      {words.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          No vocabulary items yet. Add your first word.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="text-left py-3 px-4 font-medium">Image</th>
                <th className="text-left py-3 px-4 font-medium">Word</th>
                <th className="text-left py-3 px-4 font-medium">Classifier</th>
                <th className="text-left py-3 px-4 font-medium">Category</th>
                <th className="text-left py-3 px-4 font-medium">Distractors</th>
                <th className="text-left py-3 px-4 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {words.map((word) => (
                <tr key={word.id} className="border-b border-border hover:bg-muted/20 transition-colors">
                  <td className="py-3 px-4">
                    {word.image_url ? (
                      <Image
                        src={word.image_url}
                        alt={word.word}
                        width={60}
                        height={60}
                        className="object-cover rounded"
                      />
                    ) : (
                      <div className="w-[60px] h-[60px] bg-muted rounded flex items-center justify-center text-xs text-muted-foreground">
                        No image
                      </div>
                    )}
                  </td>
                  <td className="py-3 px-4 font-medium">{word.word}</td>
                  <td className="py-3 px-4 text-muted-foreground">
                    {word.classifier || <span className="italic">—</span>}
                  </td>
                  <td className="py-3 px-4">
                    <span className="bg-muted px-2 py-0.5 rounded-full text-xs">{word.category}</span>
                  </td>
                  <td className="py-3 px-4 text-muted-foreground max-w-[200px] truncate">
                    {word.distractors.join(', ').substring(0, 50)}
                    {word.distractors.join(', ').length > 50 ? '…' : ''}
                  </td>
                  <td className="py-3 px-4">
                    <Link
                      href={`/admin/vocabulary/${word.id}/edit`}
                      className="bg-muted border border-border px-3 py-1 rounded text-sm hover:bg-muted/80 transition-colors"
                    >
                      Edit
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center mt-6">
          <span className="text-sm text-muted-foreground">
            Page {page} of {totalPages}
          </span>
          <div className="flex gap-2">
            <Link
              href={`/admin/vocabulary?category=${category}&page=${page - 1}`}
              className={`px-3 py-1.5 border border-border rounded text-sm transition-colors hover:bg-muted ${
                page <= 1 ? 'pointer-events-none opacity-50' : ''
              }`}
            >
              ← Prev
            </Link>
            <Link
              href={`/admin/vocabulary?category=${category}&page=${page + 1}`}
              className={`px-3 py-1.5 border border-border rounded text-sm transition-colors hover:bg-muted ${
                page >= totalPages ? 'pointer-events-none opacity-50' : ''
              }`}
            >
              Next →
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
