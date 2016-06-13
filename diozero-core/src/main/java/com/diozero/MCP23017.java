package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.io.Closeable;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.mcp23017.MCP23017DigitalInputDevice;
import com.diozero.internal.provider.mcp23017.MCP23017DigitalOutputDevice;
import com.diozero.internal.spi.*;
import com.diozero.util.BitManipulation;
import com.diozero.util.MutableByte;
import com.diozero.util.RuntimeIOException;

/**
 * Datasheet: <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf">http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf</a>.
 * <p>The MCP23X17 consists of multiple 8-bit configuration registers for input, output and polarity selection. The
 * system master can enable the I/Os as either inputs or outputs by writing the I/O configuration bits (IODIRA/B).
 * The data for each input or output is kept in the corresponding input or output register. The polarity of
 * the Input Port register can be inverted with the Polarity Inversion register. All registers can be read by the
 * system master.</p>
 * <p>The 16-bit I/O port functionally consists of two 8-bit ports (PORTA and PORTB). The MCP23X17 can be
 * configured to operate in the 8-bit or 16-bit modes via IOCON.BANK.</p>
 * <p>There are two interrupt pins, INTA and INTB, that can be associated with their respective ports, or can be
 * logically OR'ed together so that both pins will activate if either port causes an interrupt.
 * A special mode (Byte mode with IOCON.BANK = 0) causes the address pointer to toggle between
 * associated A/B register pairs. For example, if the BANK bit is cleared and the Address Pointer is initially set
 * to address 12h (GPIOA) or 13h (GPIOB), the pointer will toggle between GPIOA and GPIOB. Note that the
 * Address Pointer can initially point to either address in the register pair.</p>
 */
public class MCP23017 extends AbstractDeviceFactory
implements GpioDeviceFactoryInterface, InputEventListener<DigitalInputEvent>, Closeable {
	// Default I2C address
	private static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23017";
	
	// Bank=0 Registers (the default)
	private static final int BANK0_IODIRA = 0x00;
	private static final int BANK0_IODIRB = 0x01;
	private static final int[] BANK0_IODIR_REG = { BANK0_IODIRA, BANK0_IODIRB };
	private static final int BANK0_IPOLA = 0x02;
	private static final int BANK0_IPOLB = 0x03;
	private static final int[] BANK0_IPOL_REG = { BANK0_IPOLA, BANK0_IPOLB };
	private static final int BANK0_GPINTENA = 0x04;
	private static final int BANK0_GPINTENB = 0x05;
	private static final int[] BANK0_GPINTEN_REG = { BANK0_GPINTENA, BANK0_GPINTENB };
	private static final int BANK0_DEFVALA = 0x06;
	private static final int BANK0_DEFVALB = 0x07;
	private static final int[] BANK0_DEFVAL_REG = { BANK0_DEFVALA, BANK0_DEFVALB };
	private static final int BANK0_INTCONA = 0x08;
	private static final int BANK0_INTCONB = 0x09;
	private static final int[] BANK0_INTCON_REG = { BANK0_INTCONA, BANK0_INTCONB };
	private static final int BANK0_IOCONA = 0x0a;
	private static final int BANK0_IOCONB = 0x0b;
	private static final int[] BANK0_IOCON_REG = { BANK0_IOCONA, BANK0_IOCONB };
	private static final int BANK0_GPPUA = 0x0c;
	private static final int BANK0_GPPUB = 0x0d;
	private static final int[] BANK0_GPPU_REG = { BANK0_GPPUA, BANK0_GPPUB };
	private static final int BANK0_INTFA = 0x0e;
	private static final int BANK0_INTFB = 0x0f;
	private static final int[] BANK0_INTF_REG = { BANK0_INTFA, BANK0_INTFB };
	private static final int BANK0_INTCAPA = 0x10;
	private static final int BANK0_INTCAPB = 0x11;
	private static final int[] BANK0_INTCAP_REG = { BANK0_INTCAPA, BANK0_INTCAPB };
	private static final int BANK0_GPIOA = 0x12;
	private static final int BANK0_GPIOB = 0x13;
	private static final int[] BANK0_GPIO_REG = { BANK0_GPIOA, BANK0_GPIOB };
	private static final int BANK0_OLATA = 0x14;
	private static final int BANK0_OLATB = 0x15;
	private static final int[] BANK0_OLAT_REG = { BANK0_OLATA, BANK0_OLATB };
	// Bank=1 Registers
	/*
	private static final int BANK1_IODIRA = 0x00;
	private static final int BANK1_IODIRB = 0x10;
	private static final int[] BANK1_IODIR_REG = { BANK1_IODIRA, BANK1_IODIRB };
	private static final int BANK1_IPOLA = 0x01;
	private static final int BANK1_IPOLB = 0x11;
	private static final int[] BANK1_IPOL_REG = { BANK1_IPOLA, BANK1_IPOLB };
	private static final int BANK1_GPINTENA = 0x02;
	private static final int BANK1_GPINTENB = 0x12;
	private static final int[] BANK1_GPINTEN_REG = { BANK1_GPINTENA, BANK1_GPINTENB };
	private static final int BANK1_DEFVALA = 0x03;
	private static final int BANK1_DEFVALB = 0x13;
	private static final int[] BANK1_DEFVAL_REG = { BANK1_DEFVALA, BANK1_DEFVALB };
	private static final int BANK1_INTCONA = 0x04;
	private static final int BANK1_INTCONB = 0x14;
	private static final int[] BANK1_INTCON_REG = { BANK1_INTCONA, BANK1_INTCONB };
	private static final int BANK1_IOCONA = 0x05;
	private static final int BANK1_IOCONB = 0x15;
	private static final int[] BANK1_IOCON_REG = { BANK1_IOCONA, BANK1_IOCONB };
	private static final int BANK1_GPPUA = 0x06;
	private static final int BANK1_GPPUB = 0x16;
	private static final int[] BANK1_GPPU_REG = { BANK1_GPPUA, BANK1_GPPUB };
	private static final int BANK1_INTFA = 0x07;
	private static final int BANK1_INTFB = 0x17;
	private static final int[] BANK1_INTF_REG = { BANK1_INTFA, BANK1_INTFB };
	private static final int BANK1_INTCAPA = 0x08;
	private static final int BANK1_INTCAPB = 0x18;
	private static final int[] BANK1_INTCAP_REG = { BANK1_INTCAPA, BANK1_INTCAPB };
	private static final int BANK1_GPIOA = 0x09;
	private static final int BANK1_GPIOB = 0x19;
	private static final int[] BANK1_GPIO_REG = { BANK1_GPIOA, BANK1_GPIOB };
	private static final int BANK1_OLATA = 0x0a;
	private static final int BANK1_OLATB = 0x1a;
	private static final int[] BANK1_OLAT_REG = { BANK1_OLATA, BANK1_OLATB };
	*/
	
	/** Controls the direction of the data I/O. When a bit is set, the corresponding pin becomes an
	 * input. When a bit is clear, the corresponding pin becomes an output */
	private static final int[] IODIR_REG = BANK0_IODIR_REG;
	/** This register allows the user to configure the polarity on the corresponding GPIO port bits.
	 * If a bit is set, the corresponding GPIO register bit will reflect the inverted value on the pin */
	private static final int[] IPOL_REG = BANK0_IPOL_REG;
	/** The GPINTEN register controls the interrupt-on-change feature for each pin. If a bit is set,
	 * the corresponding pin is enabled for interrupt-on-change. The DEFVAL and INTCON registers
	 * must also be configured if any pins are enabled for interrupt-on-change */
	private static final int[] GPINTEN_REG = BANK0_GPINTEN_REG;
	/** The default comparison value is configured in the DEFVAL register. If enabled
	 * (via GPINTEN and INTCON) to compare against the DEFVAL register, an opposite
	 * value on the associated pin will cause an interrupt to occur */
	private static final int[] DEFVAL_REG = BANK0_DEFVAL_REG;
	/** The INTCON register controls how the associated pin value is compared for the
	 * interrupt-on-change feature. If a bit is set, the corresponding I/O pin is compared
	 * against the associated bit in the DEFVAL register. If a bit value is clear, the
	 * corresponding I/O pin is compared against the previous value */
	private static final int[] INTCON_REG = BANK0_INTCON_REG;
	/** I/O configuration register */
	private static final int[] IOCON_REG = BANK0_IOCON_REG;
	/** The GPPU register controls the pull-up resistors for the port pins. If a bit is
	 * set and the corresponding pin is configured as an input, the corresponding port pin is
	 * internally pulled up with a 100 kOhm resistor */
	private static final int[] GPPU_REG = BANK0_GPPU_REG;
	/** The INTF register reflects the interrupt condition on the port pins of any pin that is
	 * enabled for interrupts via the GPINTEN register. A 'set' bit indicates that the
	 * associated pin caused the interrupt. This register is 'read-only'. Writes to this
	 * register will be ignored */
	private static final int[] INTF_REG = BANK0_INTF_REG;
	/** The INTCAP register captures the GPIO port value at the time the interrupt occurred.
	 * The register is 'read-only' and is updated only when an interrupt occurs. The register
	 * will remain unchanged until the interrupt is cleared via a read of INTCAP or GPIO. */
	private static final int[] INTCAP_REG = BANK0_INTCAP_REG;
	/** The GPIO register reflects the value on the port. Reading from this register reads
	 * the port. Writing to this register modifies the Output Latch (OLAT) register */
	private static final int[] GPIO_REG = BANK0_GPIO_REG;
	/** The OLAT register provides access to the output latches. A read from this register
	 * results in a read of the OLAT and not the port itself. A write to this register
	 * modifies the output latches that modifies the pins configured as outputs */
	private static final int[] OLAT_REG = BANK0_OLAT_REG;
	
	/** Controls how the registers are addressed
	 * 1 = The registers associated with each port are separated into different banks
	 * 0 = The registers are in the same bank (addresses are sequential) */
	private static final byte IOCON_BANK_BIT = 7;
	/** INT Pins Mirror bit
	 * 1 = The INT pins are internally connected
	 * 0 = The INT pins are not connected. INTA is associated with PortA and INTB is associated with PortB */
	private static final byte IOCON_MIRROR_BIT = 6;
	/** Sequential Operation mode bit
	 * 1 = Sequential operation disabled, address pointer does not increment.
	 * 0 = Sequential operation enabled, address pointer increments */
	private static final byte IOCON_SEQOP_BIT = 5;
	/** Slew Rate control bit for SDA output
	 * 1 = Slew rate disabled.
	 * 0 = Slew rate enabled */
	//private static final byte IOCON_DISSLW_BIT = 4;
	/** Hardware Address Enable bit (MCP23S17 only). Address pins are always enabled on MCP23017
	 * 1 = Enables the MCP23S17 address pins.
	 * 0 = Disables the MCP23S17 address pins */
	//private static final byte IOCON_HAEN_BIT = 3;
	/** This bit configures the INT pin as an open-drain output
	 * 1 = Open-drain output (overrides the INTPOL bit).
	 * 0 = Active driver output (INTPOL bit sets the polarity) */
	private static final byte IOCON_ODR_BIT = 2;
	/** This bit sets the polarity of the INT output pin.
	 * 1 = Active-high.
	 * 0 = Active-low */
	private static final byte IOCON_INTPOL_BIT = 1;
	
	private static final int PINS_PER_PORT = 8;
	private static final int PORTS = 2;
	private static final int NUM_PINS = PORTS*PINS_PER_PORT;
	private static final int INTERRUPT_PIN_NOT_SET = -1;

	private I2CDevice device;
	private String keyPrefix;
	private DigitalInputDevice interruptPinA;
	private DigitalInputDevice interruptPinB;
	private MutableByte[] directions = { new MutableByte(), new MutableByte() };
	private MutableByte[] pullUps = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptOnChangeFlags = { new MutableByte(), new MutableByte() };
	private MutableByte[] defaultValues = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptCompareFlags = { new MutableByte(), new MutableByte() };
	private InterruptMode interruptMode = InterruptMode.DISABLED;

	public MCP23017() throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, INTERRUPT_PIN_NOT_SET, INTERRUPT_PIN_NOT_SET);
	}

	public MCP23017(int interruptPinNumber) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptPinNumber, interruptPinNumber);
	}

	public MCP23017(int interruptPinNumberA, int interruptPinNumberB) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptPinNumberA, interruptPinNumberB);
	}

	public MCP23017(int controller, int address, int interruptPinNumber) throws RuntimeIOException {
		this(controller, address, interruptPinNumber, interruptPinNumber);
	}

	public MCP23017(int controller, int address, int interruptPinNumberA, int interruptPinNumberB) throws RuntimeIOException {
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		keyPrefix = DEVICE_NAME + "-" + controller + "-" + address + "-";
		
		if (interruptPinNumberA != INTERRUPT_PIN_NOT_SET) {
			interruptPinA = new DigitalInputDevice(interruptPinNumberA, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
			if (interruptPinNumberA == interruptPinNumberB) {
				interruptMode = InterruptMode.MIRRORED;
			} else {
				interruptMode = InterruptMode.BANK_A_ONLY;
			}
		}
		
		if (interruptMode != InterruptMode.MIRRORED && interruptPinNumberB != INTERRUPT_PIN_NOT_SET) {
			interruptPinB = new DigitalInputDevice(interruptPinNumberB, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
			if (interruptMode == InterruptMode.BANK_A_ONLY) {
				interruptMode = InterruptMode.BANK_A_AND_B;
			} else {
				interruptMode = InterruptMode.BANK_B_ONLY;
			}
		}

		// Initialise
		// Read the I/O configuration value
		byte start_iocon = device.readByte(IOCON_REG[0]);
		Logger.debug("Default power-on values for IOCON: 0x{x}", Integer.toHexString(start_iocon));
		// Is there an IOCONB value?
		Logger.debug("IOCONB: 0x{x}", Integer.toHexString(device.readByte(IOCON_REG[1])));
		
		// Configure interrupts
		MutableByte iocon = new MutableByte(start_iocon);
		if (interruptMode == InterruptMode.MIRRORED) {
			// Enable interrupt mirroring
			iocon.setBit(IOCON_MIRROR_BIT);
			iocon.setBit(IOCON_INTPOL_BIT);
		} else if (interruptMode != InterruptMode.DISABLED) {
			// Disable interrupt mirroring
			iocon.unsetBit(IOCON_MIRROR_BIT);
			iocon.setBit(IOCON_INTPOL_BIT);
		}
		iocon.unsetBit(IOCON_BANK_BIT);
		iocon.setBit(IOCON_SEQOP_BIT);
		iocon.unsetBit(IOCON_ODR_BIT);
		if (!iocon.equals(start_iocon)) {
			Logger.debug("Updating IOCONA to: 0x{x}", Integer.toHexString(iocon.getValue()));
			device.writeByte(IOCON_REG[0], iocon.getValue());
		}
		
		for (int ab=0; ab<PORTS; ab++) {
			// Default all pins to output
			device.writeByte(IODIR_REG[ab], directions[ab].getValue());
			// Default to normal input polarity - IPOLA/IPOLB
			device.writeByte(IPOL_REG[ab], 0);
			// Disable interrupt-on-change for all pins
			device.writeByte(GPINTEN_REG[ab], interruptOnChangeFlags[ab].getValue());
			// Set default compare values to 0
			device.writeByte(DEFVAL_REG[ab], defaultValues[ab].getValue());
			// Disable interrupt comparison control
			device.writeByte(INTCON_REG[ab], interruptCompareFlags[ab].getValue());
			// Disable pull-up resistors
			device.writeByte(GPPU_REG[ab], pullUps[ab].getValue());
			// Set all values to off
			device.writeByte(GPIO_REG[ab], 0);
		}
		
		// Finally enable interrupt listeners
		if (interruptPinA != null) {
			Logger.debug("Setting interruptPinA ({}) consumer", Integer.valueOf(interruptPinA.getPinNumber()));
			interruptPinA.addListener(this);
		}
		if (interruptPinB != null) {
			Logger.debug("Setting interruptPinB ({}) consumer", Integer.valueOf(interruptPinB.getPinNumber()));
			interruptPinB.addListener(this);
		}
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public GpioDigitalInputDeviceInterface provisionDigitalInputPin(int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= NUM_PINS) {
			throw new IllegalArgumentException(
					"Invalid pin number (" + pinNumber + "); pin number must be 0.." + (NUM_PINS - 1));
		}
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		int port = pinNumber / PINS_PER_PORT;
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].setBit(bit);
		device.writeByte(IODIR_REG[port], directions[port].getValue());
		if (pud == GpioPullUpDown.PULL_UP) {
			pullUps[port].setBit(bit);
			device.writeByte(GPPU_REG[port], pullUps[port].getValue());
		}
		if (interruptMode != InterruptMode.DISABLED) {
			if (trigger == GpioEventTrigger.RISING) {
				defaultValues[port].unsetBit(bit);
				interruptCompareFlags[port].setBit(bit);
			} else if (trigger == GpioEventTrigger.FALLING) {
				defaultValues[port].setBit(bit);
				interruptCompareFlags[port].setBit(bit);
			} else {
				interruptCompareFlags[port].unsetBit(bit);
			}
			interruptOnChangeFlags[port].setBit(bit);
			device.writeByte(DEFVAL_REG[port], defaultValues[port].getValue());
			device.writeByte(INTCON_REG[port], interruptCompareFlags[port].getValue());
			device.writeByte(GPINTEN_REG[port], interruptOnChangeFlags[port].getValue());
		}
		
		GpioDigitalInputDeviceInterface device = new MCP23017DigitalInputDevice(this, key, pinNumber, trigger);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int pinNumber, boolean initialValue) throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= NUM_PINS) {
			throw new IllegalArgumentException(
					"Invalid pin number (" + pinNumber + "); pin number must be 0.." + (NUM_PINS - 1));
		}
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		// Nothing to do assuming that closing a pin resets it to the default output state?
		
		GpioDigitalOutputDeviceInterface device = new MCP23017DigitalOutputDevice(this, key, pinNumber);
		deviceOpened(device);
		device.setValue(initialValue);
		
		return device;
	}

	public boolean getValue(int pinNumber) throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid pin number: " + pinNumber + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; pin number must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		int port = pinNumber / PINS_PER_PORT;
		
		byte states = device.readByte(GPIO_REG[port]);
		
		return (states & bit) != 0;
	}

	public void setValue(int pinNumber, boolean value) throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid pin number: " + pinNumber + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; pin number must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		int port = pinNumber / PINS_PER_PORT;
		
		// Check the direction of the pin - can't set the value of input pins (direction bit is set)
		if (directions[port].isBitSet(bit)) {
			throw new IllegalStateException("Can't set value for input pin: " + pinNumber);
		}
		// Read the current state of this bank of GPIOs
		byte old_val = device.readByte(GPIO_REG[port]);
		byte new_val = BitManipulation.setBitValue(old_val, value, bit);
		device.writeByte(OLAT_REG[port], new_val);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close the interrupt pins
		if (interruptPinA != null) { interruptPinA.close(); }
		if (interruptPinB != null) { interruptPinB.close(); }
		// Close all open pins before closing the I2C device itself
		shutdown();
		device.close();
	}

	public void closePin(int pinNumber) throws RuntimeIOException {
		Logger.debug("closePin({})", Integer.valueOf(pinNumber));
		
		if (pinNumber < 0 || pinNumber >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid pin number: " + pinNumber + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; pin number must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		int port = pinNumber / PINS_PER_PORT;
		
		// Clean-up this pin only
		
		if (interruptOnChangeFlags[port].isBitSet(bit)) {
			interruptOnChangeFlags[port].unsetBit(bit);
			device.writeByte(GPINTEN_REG[port], interruptOnChangeFlags[port].getValue());
		}
		if (defaultValues[port].isBitSet(bit)) {
			defaultValues[port].unsetBit(bit);
			device.writeByte(DEFVAL_REG[port], defaultValues[port].getValue());
		}
		if (interruptCompareFlags[port].isBitSet(bit)) {
			interruptCompareFlags[port].unsetBit(bit);
			device.writeByte(INTCON_REG[port], interruptCompareFlags[port].getValue());
		}
		if (pullUps[port].isBitSet(bit)) {
			pullUps[port].unsetBit(bit);
			device.writeByte(GPPU_REG[port], pullUps[port].getValue());
		}
		if (directions[port].isBitSet(bit)) {
			directions[port].unsetBit(bit);
			device.writeByte(IODIR_REG[port], directions[port].getValue());
		}
	}

	@Override
	@SuppressWarnings("resource")
	public void valueChanged(DigitalInputEvent event) {
		Logger.debug("valueChanged({})", event);
		
		if (! event.getValue()) {
			Logger.info("valueChanged(): value was false - ignoring");
			return;
		}
		
		if (event.getPin() != interruptPinA.getPinNumber() && event.getPin() != interruptPinB.getPinNumber()) {
			Logger.error("Unexpected input event on pin {}", Integer.valueOf(event.getPin()));
			return;
		}
		
		synchronized (this) {
			try {
				byte[] intf = new byte[2];
				byte[] intcap = new byte[2];
				if (interruptMode == InterruptMode.MIRRORED) {
					intf[0] = device.readByte(INTF_REG[0]);
					intcap[0] = device.readByte(INTCAP_REG[0]);
					intf[1] = device.readByte(INTF_REG[1]);
					intcap[1] = device.readByte(INTCAP_REG[1]);
				} else if (interruptMode != InterruptMode.DISABLED) {
					if (event.getPin() == interruptPinA.getPinNumber()) {
						intf[0] = device.readByte(INTF_REG[0]);
						intcap[0] = device.readByte(INTCAP_REG[0]);
					} else {
						intf[1] = device.readByte(INTF_REG[1]);
						intcap[1] = device.readByte(INTCAP_REG[1]);
					}
				}
				Logger.debug("Interrupt values: [A]=(0x{}, 0x{}), [B]=(0x{}, 0x{})",
						Integer.toHexString(intf[0]), Integer.toHexString(intcap[0]),
						Integer.toHexString(intf[1]), Integer.toHexString(intcap[1]));
				for (byte bit=0; bit<7; bit++) {
					if (BitManipulation.isBitSet(intf[0], bit)) {
						boolean value = BitManipulation.isBitSet(intcap[0], bit);
						DigitalInputEvent e = new DigitalInputEvent(bit, event.getEpochTime(), event.getNanoTime(), value);
						// Notify the appropriate input device
						MCP23017DigitalInputDevice device = getInputDevice(bit);
						if (device != null) {
							device.valueChanged(e);
						}
					}
				}
				for (byte bit=0; bit<7; bit++) {
					if (BitManipulation.isBitSet(intf[1], bit)) {
						boolean value = BitManipulation.isBitSet(intcap[1], bit);
						DigitalInputEvent e = new DigitalInputEvent(bit+PINS_PER_PORT, event.getEpochTime(), event.getNanoTime(), value);
						// Notify the appropriate input device
						MCP23017DigitalInputDevice device = getInputDevice((byte)(bit+8));
						if (device != null) {
							device.valueChanged(e);
						}
					}
				}
			} catch (RuntimeIOException e) {
				// Log and ignore
				Logger.error(e, "IO error handling interrupts: {}", e);
			}
		}
	}

	private MCP23017DigitalInputDevice getInputDevice(byte pinNumber) {
		return getDevice(keyPrefix + pinNumber, MCP23017DigitalInputDevice.class);
	}
}

enum InterruptMode {
	DISABLED, BANK_A_ONLY, BANK_B_ONLY, BANK_A_AND_B, MIRRORED;
}
