import {Component, OnInit} from "@angular/core";
import "rxjs/add/operator/toPromise";

@Component({
  selector: 'app-root',
  template: `
    <h1>
      File Notifications
    </h1>
    <li *ngFor="let f of files"> {{f.path}}</li>
  `
})
export class AppComponent implements OnInit {

  chatUrl = "ws://localhost:8080/websocket/updates";
  files: Array<FileEvent> = [];
  ws = new WebSocket(this.chatUrl);

  ngOnInit(): void {
    this.ws.onopen = (ev: Event) => {
      console.log("socket session has been opened");
      this.ws.onmessage = (me: MessageEvent) => {
        const data = JSON.parse(me.data) as FileEvent;
        this.files.push(data);
        console.log(data);
      };
    };
  }
}

export interface FileEvent {
  path: string;
  sessionId: string;
}
