"use client";

import { useState } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowRightIcon,
  CheckCircleIcon,
  PaletteIcon,
  ShieldIcon,
  WalletIcon,
  UserIcon,
  Trash2Icon,
  MailIcon,
  CreditCardIcon,
  CalendarIcon,
  LayoutIcon,
} from "lucide-react";
import { useForm } from "react-hook-form";
import * as z from "zod";

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/shared/components/ui/avatar";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/shared/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/components/ui/dialog";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/shared/components/ui/form";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import { Separator } from "@/shared/components/ui/separator";
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";

const formSchema = z.object({
  walletName: z.string().min(3, "Wallet name must be at least 3 characters"),
  currency: z.string().min(1, "Currency is required"),
  initialBalance: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, "Invalid amount format"),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

export default function Home() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formSubmitted, setFormSubmitted] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      walletName: "",
      currency: "",
      initialBalance: "",
      description: "",
    },
  });

  const onSubmit = () => {
    setFormSubmitted(true);
    setTimeout(() => {
      setDialogOpen(false);
      setFormSubmitted(false);
      form.reset();
    }, 2000);
  };

  return (
    <div className="bg-background min-h-screen">
      <div className="mx-auto max-w-6xl space-y-12 px-6 py-16">
        <div className="space-y-6 text-center">
          <Badge variant="secondary" className="gap-2">
            <WalletIcon className="h-3 w-3" />
            Mithril-Vault v1.0
          </Badge>
          <h1 className="text-foreground text-5xl font-bold tracking-tight">
            Shadcn/UI Component Showcase
          </h1>
          <p className="text-muted-foreground mx-auto max-w-2xl text-lg">
            Comprehensive demonstration of all installed shadcn/ui components
            with Nord theme integration and functional examples.
          </p>
        </div>

        <nav className="bg-card rounded-lg border p-4">
          <h2 className="text-muted-foreground mb-3 text-sm font-semibold tracking-wide uppercase">
            Jump to Component
          </h2>
          <div className="flex flex-wrap gap-2">
            <Button variant="ghost" size="sm" asChild>
              <a href="#avatars">Avatars</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#badges">Badges</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#buttons">Buttons</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#cards">Cards</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#dialogs">Dialogs</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#forms">Forms</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#tables">Tables</a>
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <a href="#separators">Separators</a>
            </Button>
          </div>
        </nav>

        <Separator />

        <section id="avatars" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <UserIcon className="text-primary h-5 w-5" />
                Avatars
              </CardTitle>
              <CardDescription>
                User profile pictures with fallback initials
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Sizes</h4>
                <div className="flex items-center gap-4">
                  <Avatar className="h-8 w-8">
                    <AvatarImage
                      src="https://github.com/shadcn.png"
                      alt="Small"
                    />
                    <AvatarFallback>SM</AvatarFallback>
                  </Avatar>
                  <Avatar className="h-10 w-10">
                    <AvatarImage
                      src="https://github.com/shadcn.png"
                      alt="Medium"
                    />
                    <AvatarFallback>MD</AvatarFallback>
                  </Avatar>
                  <Avatar className="h-16 w-16">
                    <AvatarImage
                      src="https://github.com/shadcn.png"
                      alt="Large"
                    />
                    <AvatarFallback>LG</AvatarFallback>
                  </Avatar>
                  <Avatar className="h-24 w-24">
                    <AvatarImage
                      src="https://github.com/shadcn.png"
                      alt="Extra Large"
                    />
                    <AvatarFallback>XL</AvatarFallback>
                  </Avatar>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Fallback Initials</h4>
                <div className="flex items-center gap-4">
                  <Avatar>
                    <AvatarFallback className="bg-primary text-primary-foreground">
                      JD
                    </AvatarFallback>
                  </Avatar>
                  <Avatar>
                    <AvatarFallback className="bg-secondary text-secondary-foreground">
                      MV
                    </AvatarFallback>
                  </Avatar>
                  <Avatar>
                    <AvatarFallback className="bg-success text-nord-0">
                      AB
                    </AvatarFallback>
                  </Avatar>
                  <Avatar>
                    <AvatarFallback className="bg-warning text-nord-0">
                      XY
                    </AvatarFallback>
                  </Avatar>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <section id="badges" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <PaletteIcon className="text-primary h-5 w-5" />
                Badges
              </CardTitle>
              <CardDescription>
                Labels and status indicators with Nord theme variants
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Variants</h4>
                <div className="flex flex-wrap gap-2">
                  <Badge>Default</Badge>
                  <Badge variant="secondary">Secondary</Badge>
                  <Badge variant="destructive">Destructive</Badge>
                  <Badge variant="outline">Outline</Badge>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">
                  Semantic Colors (Nord)
                </h4>
                <div className="flex flex-wrap gap-2">
                  <Badge className="bg-success text-nord-0">Success</Badge>
                  <Badge className="bg-warning text-nord-0">Warning</Badge>
                  <Badge className="bg-error text-nord-6">Error</Badge>
                  <Badge className="bg-info text-nord-0">Info</Badge>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">With Icons</h4>
                <div className="flex flex-wrap gap-2">
                  <Badge className="gap-1.5">
                    <CheckCircleIcon className="h-3 w-3" />
                    Active
                  </Badge>
                  <Badge variant="secondary" className="gap-1.5">
                    <MailIcon className="h-3 w-3" />
                    Inbox (12)
                  </Badge>
                  <Badge variant="destructive" className="gap-1.5">
                    <Trash2Icon className="h-3 w-3" />
                    Deleted
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <section id="buttons" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <LayoutIcon className="text-primary h-5 w-5" />
                Buttons
              </CardTitle>
              <CardDescription>
                Interactive elements with multiple variants and sizes
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Variants</h4>
                <div className="flex flex-wrap gap-2">
                  <Button>Default</Button>
                  <Button variant="secondary">Secondary</Button>
                  <Button variant="destructive">Destructive</Button>
                  <Button variant="outline">Outline</Button>
                  <Button variant="ghost">Ghost</Button>
                  <Button variant="link">Link</Button>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Sizes</h4>
                <div className="flex flex-wrap items-center gap-2">
                  <Button size="sm">Small</Button>
                  <Button size="default">Default</Button>
                  <Button size="lg">Large</Button>
                  <Button size="icon">
                    <WalletIcon className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">States</h4>
                <div className="flex flex-wrap gap-2">
                  <Button>Enabled</Button>
                  <Button disabled>Disabled</Button>
                  <Button>
                    <ArrowRightIcon className="mr-2 h-4 w-4" />
                    With Icon
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <section id="cards" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCardIcon className="text-primary h-5 w-5" />
                Cards
              </CardTitle>
              <CardDescription>
                Container components for grouping content
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <Card>
                  <CardHeader>
                    <CardTitle>Basic Card</CardTitle>
                    <CardDescription>
                      Card with header and content
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-sm">
                      This is the content area of a basic card component.
                    </p>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>With Footer</CardTitle>
                    <CardDescription>
                      Card including footer section
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-sm">
                      Content with actions below.
                    </p>
                  </CardContent>
                  <CardFooter className="flex justify-end gap-2">
                    <Button variant="ghost" size="sm">
                      Cancel
                    </Button>
                    <Button size="sm">Confirm</Button>
                  </CardFooter>
                </Card>
              </div>
            </CardContent>
          </Card>
        </section>

        <section id="dialogs" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <LayoutIcon className="text-primary h-5 w-5" />
                Dialogs
              </CardTitle>
              <CardDescription>
                Modal overlays for focused interactions
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground text-sm">
                Click the button below to open an interactive dialog with a
                functional form.
              </p>
              <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
                <DialogTrigger asChild>
                  <Button>Create New Wallet</Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[525px]">
                  <DialogHeader>
                    <DialogTitle>Create Wallet</DialogTitle>
                    <DialogDescription>
                      Add a new financial wallet to your account. All fields are
                      validated.
                    </DialogDescription>
                  </DialogHeader>
                  <Form {...form}>
                    <form
                      onSubmit={form.handleSubmit(onSubmit)}
                      className="space-y-4"
                    >
                      <FormField
                        control={form.control}
                        name="walletName"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Wallet Name</FormLabel>
                            <FormControl>
                              <Input
                                placeholder="Personal Checking"
                                {...field}
                              />
                            </FormControl>
                            <FormDescription>
                              A unique name for this wallet
                            </FormDescription>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="currency"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Currency</FormLabel>
                            <Select
                              onValueChange={field.onChange}
                              defaultValue={field.value}
                            >
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select currency" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                <SelectItem value="USD">
                                  USD - US Dollar
                                </SelectItem>
                                <SelectItem value="EUR">EUR - Euro</SelectItem>
                                <SelectItem value="GBP">
                                  GBP - British Pound
                                </SelectItem>
                                <SelectItem value="BRL">
                                  BRL - Brazilian Real
                                </SelectItem>
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="initialBalance"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Initial Balance</FormLabel>
                            <FormControl>
                              <Input
                                type="text"
                                placeholder="1000.00"
                                {...field}
                              />
                            </FormControl>
                            <FormDescription>
                              Starting balance (e.g., 1000.00)
                            </FormDescription>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="description"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Description (Optional)</FormLabel>
                            <FormControl>
                              <Input
                                placeholder="Main checking account"
                                {...field}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <DialogFooter>
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => setDialogOpen(false)}
                        >
                          Cancel
                        </Button>
                        <Button type="submit" disabled={formSubmitted}>
                          {formSubmitted ? "Created!" : "Create Wallet"}
                        </Button>
                      </DialogFooter>
                    </form>
                  </Form>
                </DialogContent>
              </Dialog>
            </CardContent>
          </Card>
        </section>

        <section id="forms" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <MailIcon className="text-primary h-5 w-5" />
                Forms
              </CardTitle>
              <CardDescription>
                Input components with validation (see Dialog above for full
                example)
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Input Fields</h4>
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="user@example.com"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="password">Password</Label>
                    <Input
                      id="password"
                      type="password"
                      placeholder="••••••••"
                    />
                  </div>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Select Dropdown</h4>
                <Select>
                  <SelectTrigger className="w-[240px]">
                    <SelectValue placeholder="Select an option" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="checking">Checking Account</SelectItem>
                    <SelectItem value="savings">Savings Account</SelectItem>
                    <SelectItem value="credit">Credit Card</SelectItem>
                    <SelectItem value="cash">Cash</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <Separator />
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Input States</h4>
                <div className="space-y-3">
                  <Input placeholder="Default input" />
                  <Input placeholder="Disabled input" disabled />
                  <div>
                    <Input
                      placeholder="With error"
                      className="border-destructive"
                    />
                    <p className="text-destructive mt-1 text-sm">
                      This field has an error
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <section id="tables" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CalendarIcon className="text-primary h-5 w-5" />
                Tables
              </CardTitle>
              <CardDescription>
                Data display with sample financial transactions
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableCaption>Recent wallet transactions</TableCaption>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableCell className="font-medium">2025-12-30</TableCell>
                    <TableCell>Grocery Store</TableCell>
                    <TableCell>
                      <Badge variant="outline">Food</Badge>
                    </TableCell>
                    <TableCell className="text-destructive text-right">
                      -$127.50
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-medium">2025-12-28</TableCell>
                    <TableCell>Salary Deposit</TableCell>
                    <TableCell>
                      <Badge className="bg-success text-nord-0">Income</Badge>
                    </TableCell>
                    <TableCell className="text-success text-right">
                      +$3,500.00
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-medium">2025-12-26</TableCell>
                    <TableCell>Electric Bill</TableCell>
                    <TableCell>
                      <Badge variant="secondary">Utilities</Badge>
                    </TableCell>
                    <TableCell className="text-destructive text-right">
                      -$89.23
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-medium">2025-12-25</TableCell>
                    <TableCell>Restaurant</TableCell>
                    <TableCell>
                      <Badge variant="outline">Food</Badge>
                    </TableCell>
                    <TableCell className="text-destructive text-right">
                      -$65.00
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-medium">2025-12-24</TableCell>
                    <TableCell>Freelance Payment</TableCell>
                    <TableCell>
                      <Badge className="bg-success text-nord-0">Income</Badge>
                    </TableCell>
                    <TableCell className="text-success text-right">
                      +$850.00
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </section>

        <section id="separators" className="scroll-mt-16">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ShieldIcon className="text-primary h-5 w-5" />
                Separators
              </CardTitle>
              <CardDescription>
                Visual dividers for content sections
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">Horizontal</h4>
                <p className="text-muted-foreground text-sm">Content above</p>
                <Separator />
                <p className="text-muted-foreground text-sm">Content below</p>
              </div>
              <div className="space-y-2">
                <h4 className="text-sm font-semibold">In Lists</h4>
                <div className="space-y-1">
                  <p className="text-sm">First item</p>
                  <Separator />
                  <p className="text-sm">Second item</p>
                  <Separator />
                  <p className="text-sm">Third item</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <Card className="border-primary/20 bg-accent">
          <CardHeader>
            <CardTitle className="text-2xl">Component Summary</CardTitle>
            <CardDescription className="text-base">
              All 11 shadcn/ui components with Nord theme integration
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-2 text-sm md:grid-cols-3">
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Avatar (3 sizes, fallbacks)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Badge (4 variants + semantic)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Button (6 variants, 4 sizes)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Card (with all sub-components)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Dialog (functional with form)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Form (validation with Zod)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Input (multiple states)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Label (form labels)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Select (dropdown with options)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Separator (horizontal dividers)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircleIcon className="text-success h-4 w-4" />
                <span>Table (financial data example)</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
