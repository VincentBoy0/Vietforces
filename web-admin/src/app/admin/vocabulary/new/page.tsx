import Link from 'next/link'
import { redirect } from 'next/navigation'
import { createWord } from '@/lib/actions/vocabulary'

async function createAndRedirect(formData: FormData) {
  'use server'
  await createWord(formData)
  redirect('/admin/vocabulary')
}

export default function NewVocabularyPage() {
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

      <h1 className="text-2xl font-bold mb-6">Add New Word</h1>

      <form action={createAndRedirect} encType="multipart/form-data" className="space-y-5">
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
            placeholder="e.g. con mèo"
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
            placeholder="e.g. con (optional)"
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
            placeholder="e.g. animals"
            className="w-full border border-border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary text-sm"
          />
        </div>

        {/* Image */}
        <div>
          <label className="block text-sm font-medium mb-1" htmlFor="image">
            Image (optional)
          </label>
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
            Create Word
          </button>
        </div>
      </form>
    </div>
  )
}
