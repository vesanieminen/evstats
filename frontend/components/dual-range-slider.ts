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

  // Track values at drag start to know if we need to dispatch on drag end
  private dragStartLowValue: number = 0;
  private dragStartHighValue: number = 0;

  static styles = css`
    :host {
      display: block;
      position: relative;
      --slider-track-color: var(--lumo-contrast-20pct, #e0e0e0);
      --slider-range-color: var(--lumo-primary-color, #1976d2);
      --slider-thumb-color: white;
      --slider-thumb-border: var(--lumo-primary-color, #1976d2);
    }

    .slider-container {
      position: relative;
      height: 24px;
      display: flex;
      align-items: center;
    }

    .track {
      position: absolute;
      width: 100%;
      height: 6px;
      background: var(--slider-track-color);
      border-radius: 3px;
      cursor: pointer;
    }

    .range {
      position: absolute;
      height: 6px;
      background: var(--slider-range-color);
      border-radius: 3px;
      pointer-events: none;
    }

    .thumb {
      position: absolute;
      width: 18px;
      height: 18px;
      background: var(--lumo-primary-color, #1976d2);
      border: 3px solid white;
      border-radius: 50%;
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
      width: 100%;
      margin-bottom: 8px;
    }

    .label-column {
      display: flex;
      flex-direction: column;
    }

    .label-column:last-child {
      align-items: flex-end;
    }

    .label-text {
      font-size: var(--lumo-font-size-xs, 12px);
      color: var(--lumo-secondary-text-color, #6b7280);
    }

    .label-value {
      font-size: var(--lumo-font-size-l, 18px);
      font-weight: 600;
      color: var(--lumo-body-text-color, #333);
    }
  `;

  render() {
    const lowPercent = this.valueToPercent(this.lowValue);
    const highPercent = this.valueToPercent(this.highValue);

    return html`
      <div class="labels">
        <div class="label-column">
          <span class="label-text">Current</span>
          <span class="label-value">${this.lowValue}%</span>
        </div>
        <div class="label-column">
          <span class="label-text">Target</span>
          <span class="label-value">${this.highValue}%</span>
        </div>
      </div>
      <div class="slider-container">
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

    // Store initial values to compare on drag end
    this.dragStartLowValue = this.lowValue;
    this.dragStartHighValue = this.highValue;

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

      // Update UI only (no server dispatch) during drag
      if (thumb === 'low') {
        this.updateLowValueUI(Math.min(value, this.highValue - this.step * 5));
      } else {
        this.updateHighValueUI(Math.max(value, this.lowValue + this.step * 5));
      }
    };

    const handleEnd = () => {
      this.dragging = null;
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleEnd);
      document.removeEventListener('touchmove', handleMove);
      document.removeEventListener('touchend', handleEnd);

      // Dispatch events only on drag end if values changed
      if (this.lowValue !== this.dragStartLowValue) {
        this.dispatchLowValueChanged();
      }
      if (this.highValue !== this.dragStartHighValue) {
        this.dispatchHighValueChanged();
      }
    };

    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleEnd);
    document.addEventListener('touchmove', handleMove);
    document.addEventListener('touchend', handleEnd);
  }

  // Update UI only without dispatching to server (used during drag)
  private updateLowValueUI(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.lowValue) {
      this.lowValue = clampedValue;
    }
  }

  // Update UI only without dispatching to server (used during drag)
  private updateHighValueUI(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.highValue) {
      this.highValue = clampedValue;
    }
  }

  // Dispatch low value changed event to server
  private dispatchLowValueChanged() {
    this.dispatchEvent(
      new CustomEvent('low-value-changed', {
        detail: { value: this.lowValue },
        bubbles: true,
        composed: true,
      })
    );
  }

  // Dispatch high value changed event to server
  private dispatchHighValueChanged() {
    this.dispatchEvent(
      new CustomEvent('high-value-changed', {
        detail: { value: this.highValue },
        bubbles: true,
        composed: true,
      })
    );
  }

  // Set low value and immediately dispatch to server (used for track clicks)
  private setLowValue(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.lowValue) {
      this.lowValue = clampedValue;
      this.dispatchLowValueChanged();
    }
  }

  // Set high value and immediately dispatch to server (used for track clicks)
  private setHighValue(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.highValue) {
      this.highValue = clampedValue;
      this.dispatchHighValueChanged();
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'dual-range-slider': DualRangeSlider;
  }
}
