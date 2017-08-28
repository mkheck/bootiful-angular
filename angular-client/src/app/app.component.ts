import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  title = 'app';
  files: Array<FileEvent> = [];
  ws = new WebSocket('ws://localhost:8080/websocket/updates');

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
