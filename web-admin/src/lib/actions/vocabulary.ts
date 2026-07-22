'use server'

import { createAdminClient } from '@/lib/supabase/admin'
import { revalidatePath } from 'next/cache'
import type { Word } from '@/types/vocabulary'

export async function listWords(
  category: string = 'all',
  page: number = 1,
  pageSize: number = 20
): Promise<{ words: Word[]; total: number }> {
  const admin = createAdminClient()

  let query = admin.from('words').select('*', { count: 'exact' })

  if (category !== 'all') {
    query = query.eq('category', category)
  }

  query = query
    .order('created_at', { ascending: false })
    .range((page - 1) * pageSize, page * pageSize - 1)

  const { data, count, error } = await query

  if (error) {
    console.error('listWords error:', error)
    return { words: [], total: 0 }
  }

  return { words: (data as Word[]) ?? [], total: count ?? 0 }
}

export async function listCategories(): Promise<string[]> {
  const admin = createAdminClient()
  const { data } = await admin.from('words').select('category').order('category')
  return Array.from(new Set((data ?? []).map((r: { category: string }) => r.category))).filter(
    Boolean
  ) as string[]
}

export async function createWord(formData: FormData): Promise<void> {
  const word = formData.get('word') as string
  const classifier = (formData.get('classifier') as string) ?? ''
  const category = (formData.get('category') as string) ?? 'general'
  const distractorsRaw = (formData.get('distractors') as string) ?? ''
  const distractors = distractorsRaw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const imageFile = formData.get('image') as File | null

  const admin = createAdminClient()
  let imageUrl: string | null = null

  if (imageFile && imageFile.size > 0) {
    const fileBuffer = Buffer.from(await imageFile.arrayBuffer())
    const fileName =
      Date.now() + '-' + imageFile.name.replace(/[^a-zA-Z0-9._-]/g, '_')
    const { data: uploadData, error: uploadError } = await admin.storage
      .from('vocabulary-images')
      .upload(fileName, fileBuffer, { contentType: imageFile.type, upsert: false })

    if (uploadError) {
      throw new Error('Image upload failed: ' + uploadError.message)
    }
    const { data: urlData } = admin.storage
      .from('vocabulary-images')
      .getPublicUrl(uploadData.path)
    imageUrl = urlData.publicUrl
  }

  const { error } = await admin.from('words').insert({
    word,
    classifier,
    category,
    image_url: imageUrl,
    distractors,
  })

  if (error) {
    throw new Error(error.message)
  }

  revalidatePath('/admin/vocabulary')
}

export async function updateWord(id: number, formData: FormData): Promise<void> {
  const word = formData.get('word') as string
  const classifier = (formData.get('classifier') as string) ?? ''
  const category = (formData.get('category') as string) ?? 'general'
  const distractorsRaw = (formData.get('distractors') as string) ?? ''
  const distractors = distractorsRaw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const imageFile = formData.get('image') as File | null
  const existingImageUrl = (formData.get('existing_image_url') as string) || null

  const admin = createAdminClient()
  let imageUrl: string | null = existingImageUrl

  if (imageFile && imageFile.size > 0) {
    const fileBuffer = Buffer.from(await imageFile.arrayBuffer())
    const fileName =
      Date.now() + '-' + imageFile.name.replace(/[^a-zA-Z0-9._-]/g, '_')
    const { data: uploadData, error: uploadError } = await admin.storage
      .from('vocabulary-images')
      .upload(fileName, fileBuffer, { contentType: imageFile.type, upsert: false })

    if (uploadError) {
      throw new Error('Image upload failed: ' + uploadError.message)
    }
    const { data: urlData } = admin.storage
      .from('vocabulary-images')
      .getPublicUrl(uploadData.path)
    imageUrl = urlData.publicUrl
  }

  const { error } = await admin
    .from('words')
    .update({ word, classifier, category, image_url: imageUrl, distractors })
    .eq('id', id)

  if (error) {
    throw new Error(error.message)
  }

  revalidatePath('/admin/vocabulary')
  revalidatePath('/admin/vocabulary/' + id + '/edit')
}

export async function deleteWord(id: number): Promise<void> {
  const admin = createAdminClient()

  const { data: wordData } = await admin
    .from('words')
    .select('image_url')
    .eq('id', id)
    .single<{ image_url: string | null }>()

  if (wordData?.image_url) {
    const path = wordData.image_url.split('/vocabulary-images/')[1]
    if (path) {
      await admin.storage.from('vocabulary-images').remove([path])
    }
  }

  await admin.from('words').delete().eq('id', id)

  revalidatePath('/admin/vocabulary')
}
