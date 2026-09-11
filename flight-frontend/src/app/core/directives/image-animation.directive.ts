import {
  Directive,
  ElementRef,
  Renderer2,
  OnInit,
  OnDestroy,
  inject,
  PLATFORM_ID,
  Input
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Directive({
  selector: '[appImageAnimation], [appLogoAnimation]',
  standalone: true
})
export class ImageAnimationDirective implements OnInit, OnDestroy {

  private readonly el = inject(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly platformId = inject(PLATFORM_ID);

  /** Delay before animation starts (in milliseconds) */
  @Input() animationDelay = 60;

  private unlistenAnimationEnd?: () => void;
  private timerId?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const nativeEl = this.el.nativeElement as HTMLElement;
    const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ?? false;

    if (prefersReducedMotion) {
      this.renderer.addClass(nativeEl, 'img-entrance-reduced-motion');
      return;
    }

    // Set initial opacity 0 briefly to prevent flash before animation kicks in
    this.renderer.setStyle(nativeEl, 'opacity', '0');

    this.timerId = setTimeout(() => {
      this.renderer.removeStyle(nativeEl, 'opacity');
      this.renderer.addClass(nativeEl, 'img-entrance-animating');

      // Clean up the animation class when finished so standard hover transforms work flawlessly
      this.unlistenAnimationEnd = this.renderer.listen(nativeEl, 'animationend', () => {
        this.renderer.removeClass(nativeEl, 'img-entrance-animating');
        if (this.unlistenAnimationEnd) {
          this.unlistenAnimationEnd();
          this.unlistenAnimationEnd = undefined;
        }
      });
    }, this.animationDelay);
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearTimeout(this.timerId);
    }
    if (this.unlistenAnimationEnd) {
      this.unlistenAnimationEnd();
    }
  }
}
