'use client'

import React, { useRef, useEffect } from 'react'
import { gsap } from 'gsap'
import { cn } from '@/lib/utils'

export interface GlowCardProps {
  children: React.ReactNode
  className?: string
  style?: React.CSSProperties
  glowColor?: string
  glowRadius?: number
  clickEffect?: boolean
}

/**
 * GlowCard — wraps children with:
 *   1. Border glow that tracks cursor position (CSS custom properties + ::after pseudo-element)
 *   2. Ripple effect on click (gsap animated div)
 *
 * No spotlight, no tilt, no magnetism, no particles.
 */
export default function GlowCard({
  children,
  className,
  style,
  glowColor = '132, 0, 255',
  glowRadius = 220,
  clickEffect = true,
}: Readonly<GlowCardProps>) {
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = cardRef.current
    if (!el) return

    const glowState = { intensity: 0 }

    const handleMouseMove = (e: MouseEvent) => {
      const rect = el.getBoundingClientRect()
      const x = ((e.clientX - rect.left) / rect.width) * 100
      const y = ((e.clientY - rect.top) / rect.height) * 100
      el.style.setProperty('--glow-x', `${x}%`)
      el.style.setProperty('--glow-y', `${y}%`)
      el.style.setProperty('--glow-radius', `${glowRadius}px`)

      gsap.to(glowState, {
        intensity: 1,
        duration: 0.3,
        ease: 'power2.out',
        onUpdate: () => {
          el.style.setProperty('--glow-intensity', String(glowState.intensity))
          el.style.boxShadow = `0 0 ${18 * glowState.intensity}px ${6 * glowState.intensity}px rgba(${glowColor}, ${glowState.intensity * 0.22}), 0 0 ${45 * glowState.intensity}px ${12 * glowState.intensity}px rgba(${glowColor}, ${glowState.intensity * 0.1})`
        },
      })
    }

    const handleMouseLeave = () => {
      gsap.to(glowState, {
        intensity: 0,
        duration: 0.4,
        ease: 'power2.out',
        onUpdate: () => {
          el.style.setProperty('--glow-intensity', String(glowState.intensity))
          el.style.boxShadow = glowState.intensity > 0.01
            ? `0 0 ${18 * glowState.intensity}px ${6 * glowState.intensity}px rgba(${glowColor}, ${glowState.intensity * 0.22}), 0 0 ${45 * glowState.intensity}px ${12 * glowState.intensity}px rgba(${glowColor}, ${glowState.intensity * 0.1})`
            : ''
        },
      })
    }

    const handleClick = (e: MouseEvent) => {
      if (!clickEffect) return

      const rect = el.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top

      const maxDistance = Math.max(
        Math.hypot(x, y),
        Math.hypot(x - rect.width, y),
        Math.hypot(x, y - rect.height),
        Math.hypot(x - rect.width, y - rect.height)
      )

      const ripple = document.createElement('div')
      const size = maxDistance * 2
      ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        border-radius: 50%;
        background: radial-gradient(circle, rgba(${glowColor}, 0.35) 0%, rgba(${glowColor}, 0.15) 35%, transparent 70%);
        left: ${x - maxDistance}px;
        top: ${y - maxDistance}px;
        pointer-events: none;
        z-index: 10;
      `
      el.appendChild(ripple)

      gsap.fromTo(
        ripple,
        { scale: 0, opacity: 1 },
        {
          scale: 1,
          opacity: 0,
          duration: 0.75,
          ease: 'power2.out',
          onComplete: () => ripple.remove(),
        }
      )
    }

    el.addEventListener('mousemove', handleMouseMove)
    el.addEventListener('mouseleave', handleMouseLeave)
    el.addEventListener('click', handleClick)

    return () => {
      el.removeEventListener('mousemove', handleMouseMove)
      el.removeEventListener('mouseleave', handleMouseLeave)
      el.removeEventListener('click', handleClick)
    }
  }, [glowColor, glowRadius, clickEffect])

  return (
    <>
      <style>{`
        .glow-card {
          --glow-x: 50%;
          --glow-y: 50%;
          --glow-intensity: 0;
          --glow-radius: ${glowRadius}px;
        }
        .glow-card::before {
          content: '';
          position: absolute;
          inset: 0;
          border-radius: inherit;
          border: 1px solid hsl(var(--border));
          pointer-events: none;
          z-index: 1;
        }
        .glow-card::after {
          content: '';
          position: absolute;
          inset: 0;
          padding: 1px;
          background: radial-gradient(
            var(--glow-radius) circle at var(--glow-x) var(--glow-y),
            rgba(128, 128, 128, calc(var(--glow-intensity) * 1)) 0%,
            rgba(128, 128, 128, calc(var(--glow-intensity) * 0.5)) 40%,
            transparent 65%
          );
          border-radius: inherit;
          -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
          -webkit-mask-composite: xor;
          mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
          mask-composite: exclude;
          pointer-events: none;
          z-index: 2;
        }
      `}</style>
      <div
        ref={cardRef}
        className={cn('glow-card relative overflow-hidden', className)}
        style={style}
      >
        {children}
      </div>
    </>
  )
}
