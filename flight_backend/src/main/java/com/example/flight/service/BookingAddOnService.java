package com.example.flight.service;
//Manages optional add-ons

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.BookingAddOnRequestDTO;
import com.example.flight.dto.BookingAddOnResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingAddOn;
import com.example.flight.entity.BookingStatus;
import com.example.flight.repository.BookingAddOnRepository;
import com.example.flight.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingAddOnService {

    private final BookingAddOnRepository
            bookingAddOnRepository;

    private final BookingRepository bookingRepository;

    private final BookingAccessService
            bookingAccessService;

    private final AddOnPricingService
            addOnPricingService;


    // ================= ADD ADD-ON =================

    @Transactional
    public BookingAddOnResponseDTO addAddOn(
            Long bookingId,
            BookingAddOnRequestDTO request,
            String email) {


        // 1. Verify booking belongs
        //    to logged-in user

        Booking booking =
                bookingAccessService.getUserBooking(
                        bookingId,
                        email
                );


        // 2. Add-ons can only be added
        //    while booking is pending
if (booking.getStatus() != BookingStatus.PENDING) {

    throw new RuntimeException(
            "Add-ons can only be added "
            + "to a pending booking"
    );
}

        // 3. Get price from backend

        BigDecimal unitPrice =
                addOnPricingService.getUnitPrice(
                        request.getAddonType()
                );


        // 4. Calculate total add-on price

        BigDecimal totalPrice =
                unitPrice.multiply(
                        BigDecimal.valueOf(
                                request.getQuantity()
                        )
                );


        // 5. Create add-on

        BookingAddOn addOn =
                new BookingAddOn();

        addOn.setBooking(booking);

        addOn.setAddonType(
                request.getAddonType()
        );

        addOn.setDescription(
                request.getDescription()
        );

        addOn.setQuantity(
                request.getQuantity()
        );

        addOn.setUnitPrice(
                unitPrice
        );

        addOn.setTotalPrice(
                totalPrice
        );


        // 6. Save add-on

        BookingAddOn savedAddOn =
                bookingAddOnRepository.save(
                        addOn
                );


        // 7. Update booking total

        BigDecimal currentAmount =
                booking.getTotalAmount();

        BigDecimal newAmount =
                currentAmount.add(totalPrice);

        booking.setTotalAmount(newAmount);

        bookingRepository.save(booking);


        // 8. Return response

        return convertToResponse(
                savedAddOn
        );
    }


    // ================= GET ADD-ONS =================

    @Transactional(readOnly = true)
    public List<BookingAddOnResponseDTO>
            getBookingAddOns(
                    Long bookingId,
                    String email) {

        // Verify ownership

        bookingAccessService.getUserBooking(
                bookingId,
                email
        );


        return bookingAddOnRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ================= REMOVE ADD-ON =================

    @Transactional
    public void removeAddOn(
            Long bookingId,
            Long addonId,
            String email) {

        Booking booking =
                bookingAccessService.getUserBooking(
                        bookingId,
                        email
                );


       if (booking.getStatus() != BookingStatus.PENDING) {

    throw new RuntimeException(
            "Add-ons can only be added "
            + "to a pending booking"
    );
}


        BookingAddOn addOn =
                bookingAddOnRepository
                        .findById(addonId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Add-on not found"
                                )
                        );


        // Make sure the add-on belongs
        // to this booking

        if (!addOn.getBooking()
                .getBookingId()
                .equals(bookingId)) {

            throw new RuntimeException(
                    "Add-on does not belong "
                    + "to this booking"
            );
        }


        // Subtract add-on price

        BigDecimal updatedAmount =
                booking.getTotalAmount()
                        .subtract(
                                addOn.getTotalPrice()
                        );

        booking.setTotalAmount(
                updatedAmount
        );

        bookingRepository.save(booking);


        // Delete add-on

        bookingAddOnRepository.delete(
                addOn
        );
    }


    // ================= CONVERSION =================

    private BookingAddOnResponseDTO
            convertToResponse(
                    BookingAddOn addOn) {

        BookingAddOnResponseDTO response =
                new BookingAddOnResponseDTO();

        response.setAddonId(
                addOn.getAddonId()
        );

        response.setBookingId(
                addOn.getBooking()
                        .getBookingId()
        );

        response.setAddonType(
                addOn.getAddonType()
        );

        response.setDescription(
                addOn.getDescription()
        );

        response.setQuantity(
                addOn.getQuantity()
        );

        response.setUnitPrice(
                addOn.getUnitPrice()
        );

        response.setTotalPrice(
                addOn.getTotalPrice()
        );

        return response;
    }
}