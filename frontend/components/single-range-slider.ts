import {css, html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';

@customElement('single-range-slider')
export class SingleRangeSlider extends LitElement {
  @property({ type: Number }) min = 1;
  @property({ type: Number }) max = 32;
  @property({ type: Number }) value = 16;
  @property({ type: Number }) step = 1;
  @property({ type: String }) label = '';
  @property({ type: String }) unit = '';

  @state() private dragging = false;

  // Track value at drag start to know if we need to dispatch on drag end
  private dragStartValue: number = 0;

  static styles = css`
    :host {
      display: block;
      position: relative;
    }

    .slider-container {
      display: flex;
      flex-direction: column;
      gap: var(--lumo-space-s);
    }

    .slider-label {
      display: flex;
      justify-content: center;
      align-items: baseline;
    }

    .slider-value {
      font-size: var(--lumo-font-size-l, 18px);
      font-weight: 600;
      color: var(--lumo-body-text-color, #333);
    }

    .slider-track-container {
      position: relative;
      height: 24px;
      display: flex;
      align-items: center;
    }

    .slider-track {
      position: absolute;
      width: 100%;
      height: 6px;
      background: var(--lumo-contrast-20pct, #e0e0e0);
      border-radius: 3px;
      cursor: pointer;
    }

    .slider-fill {
      position: absolute;
      height: 6px;
      background: var(--lumo-primary-color, #1976d2);
      border-radius: 3px;
      pointer-events: none;
    }

    .slider-thumb {
      position: absolute;
      width: 18px;
      height: 18px;
      background: var(--lumo-primary-color, #1976d2);
      border: 3px solid white;
      border-radius: 50%;
      cursor: grab;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
      transform: translateX(-50%);
      z-index: 1;
      touch-action: none;
    }

    .slider-thumb:hover {
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
    }

    .slider-thumb:active,
    .slider-thumb.dragging {
      cursor: grabbing;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
    }
  `;

  render() {
    const percent = this.valueToPercent(this.value);

    return html`
      <div class="slider-container">
        <div class="slider-label">
          <span class="slider-value">${this.value} ${this.unit}</span>
        </div>
        <div class="slider-track-container">
          <div class="slider-track" @click=${this.handleTrackClick}></div>
          <div
            class="slider-fill"
            style="left: 0; width: ${percent}%"
          ></div>
          <div
            class="slider-thumb ${this.dragging ? 'dragging' : ''}"
            style="left: ${percent}%"
            @mousedown=${this.startDrag}
            @touchstart=${this.startDrag}
          ></div>
        </div>
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

  // Handle click on track - dispatches immediately
  private handleTrackClick(e: MouseEvent) {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const percent = ((e.clientX - rect.left) / rect.width) * 100;
    const value = this.percentToValue(percent);
    this.setValue(value);
  }

  private startDrag(e: MouseEvent | TouchEvent) {
    e.preventDefault();
    this.dragging = true;

    // Store initial value to compare on drag end
    this.dragStartValue = this.value;

    const handleMove = (moveEvent: MouseEvent | TouchEvent) => {
      const clientX =
        moveEvent instanceof MouseEvent
          ? moveEvent.clientX
          : moveEvent.touches[0].clientX;

      const track = this.shadowRoot?.querySelector('.slider-track') as HTMLElement;
      if (!track) return;

      const rect = track.getBoundingClientRect();
      const percent = Math.max(
        0,
        Math.min(100, ((clientX - rect.left) / rect.width) * 100)
      );
      const value = this.percentToValue(percent);

      // Update UI only (no server dispatch) during drag
      this.updateValueUI(value);
    };

    const handleEnd = () => {
      this.dragging = false;
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleEnd);
      document.removeEventListener('touchmove', handleMove);
      document.removeEventListener('touchend', handleEnd);

      // Dispatch event only on drag end if value changed
      if (this.value !== this.dragStartValue) {
        this.dispatchValueChanged();
      }
    };

    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleEnd);
    document.addEventListener('touchmove', handleMove);
    document.addEventListener('touchend', handleEnd);
  }

  // Update UI only without dispatching to server (used during drag)
  private updateValueUI(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.value) {
      this.value = clampedValue;
    }
  }

  // Dispatch value changed event to server
  private dispatchValueChanged() {
    this.dispatchEvent(
      new CustomEvent('value-changed', {
        detail: { value: this.value },
        bubbles: true,
        composed: true,
      })
    );
  }

  // Set value and immediately dispatch to server (used for track clicks)
  private setValue(value: number) {
    const clampedValue = Math.max(this.min, Math.min(value, this.max));
    if (clampedValue !== this.value) {
      this.value = clampedValue;
      this.dispatchValueChanged();
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'single-range-slider': SingleRangeSlider;
  }
}
