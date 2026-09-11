import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ChatbotWidgetComponent } from './shared/components/chatbot-widget/chatbot-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ChatbotWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'flight-frontend';
}
