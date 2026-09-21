export function renderLinkContent(content: string): string {
  // Regular expression to match Markdown links: [text](url)
  const markdownLinkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;

  return content.replace(markdownLinkRegex, (match, text, url) => {
    // Sanitize the URL and text to prevent XSS
    const sanitizedUrl = url.replace(/[<>"']/g, "")
    const sanitizedText = text.replace(/[<>"']/g, "")

    return `<a href="${sanitizedUrl}" target="_blank" rel="noopener noreferrer">${sanitizedText}</a>`
  })
}
