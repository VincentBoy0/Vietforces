import Link from 'next/link'
import Image from 'next/image'
import { notFound, redirect } from 'next/navigation'
import { createAdminClient } from '@/lib/supabase/admin'
import { updateWord, deleteWord } from '@/lib/actions/vocabulary'
import type { Word } from '@/types/vocabulary'

export default async function EditVocabularyPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const wordId = parseInt(id, 10)

  if (isNaN(wordId)) {
    notFound()
  }

  const admin = createAdminClient()
  const { data: word, error } = await admin
    .from('words')
    .select('*')
    .eq('id', wordId)
    .single<Word>()

  if (!word || error) {
    notFound()
  }

  const updateWordWithId = updateWord.bind(null, wordId)

  async function deleteAndRedirect() {
    'use server'
    await deleteWord(wordId)
    redirect('/admin/vocabulary')
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-6">
        <Link
          href="/admin/vocabulary"
          className="text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Back to Vocabulary
        </Link>
      </div>

      <h1 className="text-2xl font-bold mb-6">Edit Word: {word.word}</h1>

      <div className="flex gap-8">
        {/* Edit Form */}
        <div className="flex-1">
          <form action={updateWordWithId} encType="multipart/form-data" className="space-y-5">
            <input type="hidden" name="existing_image_url" value={word.image_url ?? ''} />

            {/* Word */}
            <div>
              <label className="block text-sm font-medium mb-1" htmlFor="word">
                Word <span className="text-danger">*</span>
              </label>
              <input
                id="word"
                name="word"
                type="text"
                required
                defaultValue={word.word}
                className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm"
              />
            </div>

            {/* Classifier */}
            <div>
              <label className="block text-sm font-medium mb-1" htmlFor="classifier">
                Classifier
              </label>
              <input
                id="classifier"
                name="classifier"
                type="text"
                defaultValue={word.classifier ?? ''}
                className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm"
              />
            </div>

            {/* Category */}
            <div>
              <label className="block text-sm font-medium mb-1" htmlFor="category">
                Category <span className="text-danger">*</span>
              </label>
              <input
                id="category"
                name="category"
                type="text"
                required
                defaultValue={word.category}
                className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm"
              />
            </div>

            {/* Image replacement */}
            <div>
              <label className="block text-sm font-medium mb-1" htmlFor="image">
                Replace Image (optional)
              </label>
              {word.image_url && (
                <div className="mb-2">
                  <p className="text-xs text-muted-foreground mb-1">Current image:</p>
                  <Image
                    src={word.image_url}
                    alt={word.word}
                    width={80}
                    height={80}
                    className="object-cover rounded border border-border"
                  />
                </div>
              )}
              <input
                id="image"
                name="image"
                type="file"
                accept="image/*"
                className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-sm file:bg-muted file:cursor-pointer"
              />
            </div>

            {/* Distractors */}
            <div>
              <label className="block text-sm font-medium mb-1" htmlFor="distractors">
                Distractors
              </label>
              <textarea
                id="distractors"
                name="distractors"
                rows={3}
                defaultValue={word.distractors.join(', ')}
                placeholder="mèo, chó, chim (comma-separated)"
                className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm resize-none"
              />
              <p className="text-xs text-muted-foreground mt-1">
                Separate with commas. Used as wrong choices in games.
              </p>
            </div>

            {/* Submit */}
            <div className="pt-2">
              <button
                type="submit"
                className="bg-primary text-white px-6 py-2 rounded hover:opacity-90 transition-opacity text-sm font-medium"
              >
                Save Changes
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="mt-8 pt-6 border-t border-border">
        <h2 className="text-lg font-semibold text-danger mb-2">Danger Zone</h2>
        <p className="text-sm text-muted-foreground mb-4">
          This will permanently delete this word and its image.
        </p>
        <form action={deleteAndRedirect}>
          <button
            type="submit"
            className="bg-danger text-white px-4 py-2 rounded hover:opacity-90 transition-opacity text-sm font-medium"
          >
            Delete Word
          </button>
        </form>
      </div>
    </div>
  )
}
