import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <div>
      <ol>
        <li *ngFor="let f of files">
          {{f.sessionId}} <b> {{f.path}}</b>
        </li>
      </ol>
    </div>
  `
})
export class AppComponent implements OnInit {

  files: Array<FileEvent> = [];
  private ws = new WebSocket('ws://localhost:8080/websocket/updates');

  ngOnInit(): void {
    this.ws.onopen = (openEvent: Event) => {
      this.ws.onmessage = (msgEvent: MessageEvent) => {
        const data = JSON.parse(msgEvent.data) as FileEvent;
        this.files.push(data);
      };
    };
  }
}

export interface FileEvent {
  sessionId: string;
  path: string;
}
