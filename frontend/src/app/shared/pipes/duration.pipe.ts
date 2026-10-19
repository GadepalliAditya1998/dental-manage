import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'duration',
  standalone: false
})
export class DurationPipe implements PipeTransform {
  transform(valueMs: number | undefined | null): string {
    if (valueMs === undefined || valueMs === null || isNaN(valueMs)) {
      return '0s';
    }
    const totalSeconds = Math.floor(valueMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    }
    return `${seconds}s`;
  }
}
