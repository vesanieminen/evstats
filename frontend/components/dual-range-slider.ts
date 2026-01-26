import { LitElement, html, css, PropertyValues } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('dual-range-slider')
export class DualRangeSlider extends LitElement {
  @property({ type: Number }) min = 0;
  @property({ type: Number }) max = 100;
  @property({ type: Number, attribute: 'low-value' }) lowValue = 20;
  @property({ type: Number, attribute: 'high-value' }) highValue = 80;
  @property({ type: Number }) step = 1;

  @state() private dragging: 'low' | 'high' | null = null;

  static styles = css`
    :host {
      display: block;
      position: relative;
      height: 64px;
      padding-top: 8px;
      --slider-track-color: var(--lumo-contrast-20pct, #e0e0e0);
      --slider-range-color: var(--lumo-primary-color, #1976d2);
      --slider-thumb-color: white;
      --slider-thumb-border: var(--lumo-primary-color, #1976d2);
    }

    .track {
      position: absolute;
      width: 100%;
      height: 6px;
      background: var(--slider-track-color);
      border-radius: 3px;
      top: 20px;
      cursor: pointer;
    }

    .range {
      position: absolute;
      height: 6px;
      background: var(--slider-range-color);
      border-radius: 3px;
      top: 20px;
      pointer-events: none;
    }

    .thumb {
      position: absolute;
      width: 18px;
      height: 18px;
      background: var(--lumo-primary-color, #1976d2);
      border: 3px solid white;
      border-radius: 50%;
      top: 14px;
      cursor: grab;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
      transform: translateX(-50%);
      z-index: 2;
      touch-action: none;
    }

    .thumb:hover {
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
    }

    .thumb:active,
    .thumb.dragging {
      cursor: grabbing;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
    }

    .thumb.low {
      background: var(--lumo-primary-color, #1976d2);
    }

    .thumb.high {
      background: var(--lumo-primary-color, #1976d2);
    }

    .labels {
      display: flex;
      justify-content: space-between;
      position: absolute;
      width: 100%;
      bottom: 0;
      font-size: var(--lumo-font-size-s, 14px);
      font-weight: 500;
      color: var(--lumo-body-text-color, #333);
    }
  `;

  render() {
    const lowPercent = this.valueToPercent(this.lowValue);
    const highPercent = this.valueToPercent(this.highValue);

    return html`
      <div class="track" @click=${this.handleTrackClick}></div>
      <div
        class="range"
        style="left: ${lowPercent}%; width: ${highPercent - lowPercent}%"
      ></div>
      <div
        class="thumb low ${this.dragging === 'low' ? 'dragging' : ''}"
        style="left: ${lowPercent}%"
        @mousedown=${(e: MouseEvent) => this.startDrag(e, 'low')}
        @touchstart=${(e: TouchEvent) => this.startDrag(e, 'low')}
      ></div>
      <div
        class="thumb high ${this.dragging === 'high' ? 'dragging' : ''}"
        style="left: ${highPercent}%"
        @mousedown=${(e: MouseEvent) => this.startDrag(e, 'high')}
        @touchstart=${(e: TouchEvent) => this.startDrag(e, 'high')}
      ></div>
      <div class="labels">
        <span>${this.lowValue}%</span>
        <span>${this.highValue}%</span>
      </div>
    `;
  }

  private valueToPercent(value: number): number {
    return ((value - this.min) / (this.max - this.min)) * 100;
  }

  private percentToValue(percent: number): number {
    const value = (percent / 100) * (this.max - this.min) + this.min;
    return Math.round(value / this.step) * this.step;
  }

  private handleTrackClick(e: MouseEvent) {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const percent = ((e.clientX - rect.left) / rect.width) * 100;
    const value = this.percentToValue(percent);

    const distToLow = Math.abs(value - this.lowValue);
    const distToHigh = Math.abs(value - this.highValue);

    if (distToLow < distToHigh) {
      this.setLowValue(Math.min(value, this.highValue - this.step * 5));
    } else {
      this.setHighValue(Math.max(value, this.lowValue + this.step * 5));
    }
  }

  private startDrag(e: MouseEvent | TouchEvent, thumb: 'low' | 'high') {
    e.preventDefault();
    this.dragging = thumb;

    const handleMove = (moveEvent: MouseEvent | TouchEvent) => {
      const clientX =
        moveEvent instanceof MouseEvent
          ? moveEvent.clientX
          : moveEvent.touches[0].clientX;

      const track = this.shadowRoot?.querySelector('.track') as HTMLElement;
      if (!track) return;

      const rect = track.getBoundingClientRect();
      const percent = Math.max(
        0,
        Math.min(100, ((clientX - rect.left) / rect.width) * 100)
      );
      const value = this.percentToValue(percent);

      if (thumb === 'low') {
        this.setLowValue(Math.min(value, this.highValue - this.step * 5));
      } else {
        this.setHighValue(Math.max(value, this.lowValue + this.step * 5));
      }
    };

    const handleEnd = () => {
      this.dragging = null;
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleEnd);
      document.removeEventListener('touchmove', handleMove);
      document.removeEventListener('touchend', handleEnd);
    };

    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleEnd);
    document.addEventListener('touchmove', handleMove);
    document.addEventListener('touchend', handleEnd);
  }

  private setLowValue(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.lowValue) {
      this.lowValue = clampedValue;
      this.dispatchEvent(
        new CustomEvent('low-value-changed', {
          detail: { value: this.lowValue },
          bubbles: true,
          composed: true,
        })
      );
    }
  }

  private setHighValue(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.highValue) {
      this.highValue = clampedValue;
      this.dispatchEvent(
        new CustomEvent('high-value-changed', {
          detail: { value: this.highValue },
          bubbles: true,
          composed: true,
        })
      );
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'dual-range-slider': DualRangeSlider;
  }
}
