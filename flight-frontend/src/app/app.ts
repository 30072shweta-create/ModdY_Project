import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
<<<<<<< HEAD
import { ChatbotWidgetComponent } from './shared/components/chatbot-widget/chatbot-widget.component';
=======
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f

@Component({
  selector: 'app-root',
  standalone: true,
<<<<<<< HEAD
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ChatbotWidgetComponent],
=======
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'flight-frontend';
}
