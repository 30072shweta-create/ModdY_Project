import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ChatDomain, DomainResponse } from '../../../core/models/chat.model';
import { ChatService } from '../../../core/services/chat.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
}

@Component({
  selector: 'app-chatbot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot-widget.component.html',
  styleUrls: ['./chatbot-widget.component.scss']
})
export class ChatbotWidgetComponent implements OnInit {
  isOpen = false;
  isLoading = false;
  message = '';
  conversationId = this.getConversationId();
  domains: DomainResponse[] = [];
  selectedDomain: ChatDomain = 'BOOKING';
  messages: ChatMessage[] = [
    {
      role: 'assistant',
      text: 'Hello. I can help with flight booking, payment, cancellation, pricing, and seat questions.'
    }
  ];

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    this.chatService.getDomains().subscribe({
      next: domains => {
        this.domains = domains;
        if (domains.length > 0) {
          this.selectedDomain = domains[0].value;
        }
      },
      error: () => {
        this.domains = [
          { name: 'Booking', value: 'BOOKING' },
          { name: 'Payment', value: 'PAYMENT' },
          { name: 'Cancellation and Refund', value: 'CANCELLATION_AND_REFUND' }
        ];
      }
    });
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
  }

  sendMessage(): void {
    const trimmedMessage = this.message.trim();

    if (!trimmedMessage || this.isLoading) {
      return;
    }

    this.messages = [...this.messages, { role: 'user', text: trimmedMessage }];
    this.message = '';
    this.isLoading = true;

    this.chatService.sendMessage({
      conversationId: this.conversationId,
      domain: this.selectedDomain,
      message: trimmedMessage
    })
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: response => {
          this.messages = [...this.messages, { role: 'assistant', text: response.answer }];
        },
        error: () => {
          this.messages = [
            ...this.messages,
            { role: 'assistant', text: 'Chat is unavailable right now. Please try again later.' }
          ];
        }
      });
  }

  private getConversationId(): string {
    const storageKey = 'flight-chat-conversation-id';
    const existingId = localStorage.getItem(storageKey);

    if (existingId) {
      return existingId;
    }

    const newId = crypto.randomUUID();
    localStorage.setItem(storageKey, newId);
    return newId;
  }
}
